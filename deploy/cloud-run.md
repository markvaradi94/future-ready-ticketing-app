# Deploying to Cloud Run

The commands that put the three services on Cloud Run, from an empty Google Cloud project. Run them from the repository root, in Git Bash or another POSIX shell. No value in this file belongs to a real project: set the variables first.

> **Git Bash on Windows rewrites arguments that look like Unix paths.** `httpGet.path=/actuator/health/readiness` reaches Cloud Run as `C:/Program Files/Git/actuator/...`, and the startup probe fails after two minutes. Prefix those commands with `MSYS2_ARG_CONV_EXCL="httpGet.path="`, as below. Don't set `MSYS2_ARG_CONV_EXCL='*'` or `MSYS_NO_PATHCONV=1`: they break the `gcloud` launcher itself.

You need the same six variables as `compose.cloud.yml`: `MONGODB_HOST`, `MONGODB_USERNAME`, `MONGODB_PASSWORD`, `CLOUDAMQP_HOST`, `CLOUDAMQP_USERNAME`, `CLOUDAMQP_PASSWORD` (and `CLOUDAMQP_VHOST` only if your virtual host is not your username). Never print them.

```bash
export PROJECT_ID=your-project-id
export REGION=europe-west1
export REPO=$REGION-docker.pkg.dev/$PROJECT_ID/ticketing
gcloud config set project $PROJECT_ID
gcloud config set run/region $REGION
```

## 1. The APIs

A new project needs these APIs enabled once. Enabling one that is already on does nothing, so run the whole list.

| API | What it's for | Session |
| --- | --- | --- |
| `run.googleapis.com` | Cloud Run: the services and their revisions | 9 |
| `artifactregistry.googleapis.com` | Artifact Registry: the images | 9 |
| `secretmanager.googleapis.com` | Secret Manager: the two passwords | 9 |
| `iam.googleapis.com` | service accounts and their roles | 9 |
| `logging.googleapis.com` | Cloud Logging: the services' logs | 9 |
| `iamcredentials.googleapis.com` | short-lived credentials for service accounts (Workload Identity Federation) | 10 |
| `sts.googleapis.com` | Security Token Service: GitHub's token exchanged for a Google one | 10 |

```bash
gcloud services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com \
    iam.googleapis.com logging.googleapis.com iamcredentials.googleapis.com sts.googleapis.com
```

## 2. The image repository

```bash
gcloud artifacts repositories create ticketing --repository-format=docker --location=$REGION
gcloud auth configure-docker $REGION-docker.pkg.dev
```

## 3. The secrets: only the passwords

Each value comes from an environment variable you already have, so it is never typed on the command line or kept in your shell history. `--data-file=-` reads it from the pipe. The hosts and usernames are not secrets: they are plain settings in section 6.

```bash
printf '%s' "$MONGODB_PASSWORD" | gcloud secrets create mongodb-password --data-file=- --replication-policy=automatic
printf '%s' "$CLOUDAMQP_PASSWORD" | gcloud secrets create cloudamqp-password --data-file=- --replication-policy=automatic
gcloud secrets list                      # names only; never print the values
```

## 4. One service account per service

Each service runs with its own identity and only the roles it needs. The backends may read the two passwords; the gateway may call the backends once they exist (section 7).

```bash
gcloud iam service-accounts create ticketing-run --display-name="ticketing-service on Cloud Run"
gcloud iam service-accounts create notification-run --display-name="notification-service on Cloud Run"
gcloud iam service-accounts create gateway-run --display-name="api-gateway on Cloud Run"

for secret in mongodb-password cloudamqp-password; do
  for sa in ticketing-run notification-run; do
    gcloud secrets add-iam-policy-binding $secret \
        --member="serviceAccount:$sa@$PROJECT_ID.iam.gserviceaccount.com" \
        --role=roles/secretmanager.secretAccessor
  done
done
```

## 5. The images

Build the three images from the repository root and push them, tagged with the session. Session 10's pipeline tags them with the commit SHA instead.

```bash
for pair in ticketing-service:8085 notification-service:8086 api-gateway:8080; do
  service=${pair%%:*}
  docker build --platform linux/amd64 --build-arg SERVICE=$service --build-arg PORT=${pair#*:} -t $REPO/$service:session-09 .
  docker push $REPO/$service:session-09
done
```

## 6. The backends, private

Neither backend accepts unauthenticated calls (`--no-allow-unauthenticated`): only the gateway calls them. Both run the `cloud` profile, which reads the same variables `compose.cloud.yml` uses: the hosts and usernames as plain settings, the passwords from Secret Manager. Each has a startup probe on Actuator's readiness. Nothing is seeded on Atlas.

`notification-service` consumes RabbitMQ and gets no HTTP traffic. By default Cloud Run gives an instance CPU only during a request and scales to zero, so it would never consume. It keeps one instance running with CPU always allocated (`--no-cpu-throttling --min-instances 1`). That instance costs money for as long as it runs: see section 8.

If your CloudAMQP virtual host is not your username, add `CLOUDAMQP_VHOST=...` to `--set-env-vars`.

```bash
MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy notification-service \
    --image $REPO/notification-service:session-09 \
    --service-account notification-run@$PROJECT_ID.iam.gserviceaccount.com \
    --no-allow-unauthenticated --port 8086 --cpu 1 --memory 512Mi \
    --no-cpu-throttling --min-instances 1 --max-instances 1 \
    --set-env-vars SPRING_PROFILES_ACTIVE=cloud,MONGODB_HOST=$MONGODB_HOST,MONGODB_USERNAME=$MONGODB_USERNAME,CLOUDAMQP_HOST=$CLOUDAMQP_HOST,CLOUDAMQP_USERNAME=$CLOUDAMQP_USERNAME \
    --set-secrets MONGODB_PASSWORD=mongodb-password:latest,CLOUDAMQP_PASSWORD=cloudamqp-password:latest \
    --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8086,periodSeconds=5,timeoutSeconds=3,failureThreshold=24

MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy ticketing-service \
    --image $REPO/ticketing-service:session-09 \
    --service-account ticketing-run@$PROJECT_ID.iam.gserviceaccount.com \
    --no-allow-unauthenticated --port 8085 --cpu 1 --memory 512Mi \
    --min-instances 0 --max-instances 2 \
    --set-env-vars SPRING_PROFILES_ACTIVE=cloud,MONGODB_HOST=$MONGODB_HOST,MONGODB_USERNAME=$MONGODB_USERNAME,CLOUDAMQP_HOST=$CLOUDAMQP_HOST,CLOUDAMQP_USERNAME=$CLOUDAMQP_USERNAME \
    --set-secrets MONGODB_PASSWORD=mongodb-password:latest,CLOUDAMQP_PASSWORD=cloudamqp-password:latest \
    --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8085,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

Check them: without credentials they answer 403, and with your identity token they answer.

```bash
TICKETING_URL=$(gcloud run services describe ticketing-service --format='value(status.url)')
curl -i $TICKETING_URL/events                                                               # 403
curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" $TICKETING_URL/events   # 200
gcloud run services logs read notification-service --limit 20                               # started, connected
```

## 7. The gateway, the only public URL

The gateway runs as `gateway-run`, which may call both backends (`roles/run.invoker`). With `GATEWAY_CLOUDRUNAUTH_ENABLED=true` it asks Cloud Run's metadata server for an ID token for each backend's URL and sends it as `Authorization: Bearer ...` (`IdentityTokens`). Any `Authorization` header from the client is replaced, never forwarded.

```bash
for service in ticketing-service notification-service; do
  gcloud run services add-iam-policy-binding $service \
      --member="serviceAccount:gateway-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/run.invoker
done

TICKETING_URL=$(gcloud run services describe ticketing-service --format='value(status.url)')
NOTIFICATIONS_URL=$(gcloud run services describe notification-service --format='value(status.url)')

MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy api-gateway \
    --image $REPO/api-gateway:session-09 \
    --service-account gateway-run@$PROJECT_ID.iam.gserviceaccount.com \
    --allow-unauthenticated --port 8080 --cpu 1 --memory 512Mi \
    --min-instances 0 --max-instances 2 \
    --set-env-vars GATEWAY_TICKETINGURI=$TICKETING_URL,GATEWAY_NOTIFICATIONSURI=$NOTIFICATIONS_URL,GATEWAY_CLOUDRUNAUTH_ENABLED=true \
    --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8080,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

> **New IAM bindings take a few minutes to apply.** Right after the `add-iam-policy-binding` calls, the gateway's requests can still get 403 from the backends (it took about 4 minutes on 2026-10-29). Wait, then try again; don't start changing the configuration.

Check it end to end. Nothing is seeded on Atlas, so create an event first:

```bash
GATEWAY_URL=$(gcloud run services describe api-gateway --format='value(status.url)')
curl -i -X POST $GATEWAY_URL/events -H "Content-Type: application/json" \
    -d '{"name":"Cloud Night","venueName":"BT Arena","city":"Cluj","startsAt":"2030-06-12T20:00:00","capacity":100}'
curl -i -X POST $GATEWAY_URL/events/<the event's id>/bookings -H "Content-Type: application/json" \
    -H "X-Correlation-Id: cloud-test-1" -d '{"customerEmail":"ana@mail.ro","seats":2}'
curl $GATEWAY_URL/notifications
gcloud logging read 'textPayload:"cloud-test-1"' --limit 20 --format='value(resource.labels.service_name,textPayload)'
```

In Postman: set `baseUrl` and `notificationsUrl` to `$GATEWAY_URL`; the folder "Call off an event with bookings" and `/notifications` work on an empty database (the rest use the events seeded only locally).

## 8. Costs: scale down when you're done

`notification-service` is the only service that runs when nobody uses it: a few cents an hour (1 vCPU, 512 MiB, CPU always allocated). The others scale to zero by themselves. When you're done, scale it to zero; scale it up again before the next demo.

```bash
gcloud run services update notification-service --min-instances 0      # done for today
gcloud run services update notification-service --min-instances 1      # before the next demo
```

To remove everything at the end of the course (the services, the images, the secrets, the pipeline's access and the service accounts, or the whole project), follow `deploy/cleanup.md`.
