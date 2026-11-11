# From zero to a pipeline that deploys

Every command, in order, that takes your course project from "builds on my laptop" to "a push to `main` deploys it to Cloud Run". It is Session 9 (`deploy/cloud-run.md`) and Session 10 (`deploy/github-actions.md`) in one list, for your own GitHub repository and your own Google Cloud project.

**How to use it**

- Run everything in **Git Bash**, from the root of your repository.
- One command per line. Paste one line, press Enter, read what it printed, then the next one.
- `<...>` is a value you replace, without the angle brackets.
- "already exists" when you run a command a second time is fine: carry on with the next one.
- Never paste a password into a command, a file or a chat. Section 0 reads them without showing them.

**What you need before you start**

- Docker Desktop running, `gcloud` and `gh` (the GitHub CLI) installed: `gcloud --version`, `gh --version`.
- Your course project pushed to your own GitHub repository, with `./gradlew build` green.
- Your Atlas cluster and CloudAMQP instance from Session 6, with their hosts, usernames and passwords. In Atlas, **Network Access** must allow `0.0.0.0/0`: Cloud Run has no fixed address.
- A Google account with billing set up (the free trial is fine).

---

## 0. The variables (again in every new terminal)

A new Git Bash window forgets them. If a command later complains about an empty value, run this section again.

```bash
export PROJECT_ID=<your-project-id>
export REGION=europe-west1
export REPO=$REGION-docker.pkg.dev/$PROJECT_ID/ticketing
export MONGODB_HOST=<your-cluster>.mongodb.net
export MONGODB_USERNAME=<your Atlas database user>
export CLOUDAMQP_HOST=<your-instance>.rmq.cloudamqp.com
export CLOUDAMQP_USERNAME=<your CloudAMQP user>
```

`PROJECT_ID` must be unique in all of Google Cloud: 6 to 30 lowercase letters, digits and dashes, for example `ticketing-ana-2026`.

The passwords: each command waits for you to type or paste the password. Nothing shows while you do; press Enter at the end.

```bash
read -rsp "MongoDB password: " MONGODB_PASSWORD; echo; export MONGODB_PASSWORD
read -rsp "CloudAMQP password: " CLOUDAMQP_PASSWORD; echo; export CLOUDAMQP_PASSWORD
echo "MongoDB ${#MONGODB_PASSWORD} characters, CloudAMQP ${#CLOUDAMQP_PASSWORD} characters"
```

The last line prints only the lengths. `0 characters` means try again.

## 1. Sign in

A separate `gcloud` configuration keeps this project apart from anything else on your laptop.

```bash
gcloud config configurations create final-project
gcloud auth login
gh auth login
export GITHUB_REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo $GITHUB_REPO
```

`gh repo view` reads the repository of the folder you are in. `echo` must print `your-github-user/your-repository`; if it doesn't, you are in the wrong folder.

## 2. The project and its billing

```bash
gcloud projects create $PROJECT_ID
gcloud config set project $PROJECT_ID
gcloud config set run/region $REGION
gcloud billing accounts list
gcloud billing projects link $PROJECT_ID --billing-account=<ACCOUNT_ID from the list>
gcloud billing projects describe $PROJECT_ID --format="value(billingEnabled)"
```

The last command must print `True`.

**A budget alert** (once per billing account; skip it if you made one in Session 9): console → search `Budgets` → **Budgets & alerts** → **Create budget** → scope **All projects** → a few euros → keep the 50 / 90 / 100 % thresholds → **Finish**.

## 3. The APIs

One command, one line. It takes a minute or two.

```bash
gcloud services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com iam.googleapis.com logging.googleapis.com iamcredentials.googleapis.com sts.googleapis.com
```

## 4. The image repository

```bash
gcloud artifacts repositories create ticketing --repository-format=docker --location=$REGION
gcloud auth configure-docker $REGION-docker.pkg.dev --quiet
```

## 5. The two secrets

Only the passwords are secret. The hosts and usernames are plain settings (section 8).

```bash
printf '%s' "$MONGODB_PASSWORD" | gcloud secrets create mongodb-password --data-file=- --replication-policy=automatic
printf '%s' "$CLOUDAMQP_PASSWORD" | gcloud secrets create cloudamqp-password --data-file=- --replication-policy=automatic
gcloud secrets list
```

`gcloud secrets list` shows the two names. Never run `gcloud secrets versions access`: it prints the value.

## 6. One service account per service

```bash
gcloud iam service-accounts create ticketing-run --display-name="ticketing-service on Cloud Run"
gcloud iam service-accounts create notification-run --display-name="notification-service on Cloud Run"
gcloud iam service-accounts create gateway-run --display-name="api-gateway on Cloud Run"
```

The two backends may read the two passwords:

```bash
gcloud secrets add-iam-policy-binding mongodb-password --member="serviceAccount:ticketing-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/secretmanager.secretAccessor
gcloud secrets add-iam-policy-binding mongodb-password --member="serviceAccount:notification-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/secretmanager.secretAccessor
gcloud secrets add-iam-policy-binding cloudamqp-password --member="serviceAccount:ticketing-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/secretmanager.secretAccessor
gcloud secrets add-iam-policy-binding cloudamqp-password --member="serviceAccount:notification-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/secretmanager.secretAccessor
```

## 7. The first images, built on your laptop

The pipeline only swaps the image of a service that already exists, so the first deploy is by hand. `--platform linux/amd64` matters on an Apple Silicon Mac: Cloud Run runs amd64.

```bash
docker build --platform linux/amd64 --build-arg SERVICE=ticketing-service --build-arg PORT=8085 -t $REPO/ticketing-service:first .
docker build --platform linux/amd64 --build-arg SERVICE=notification-service --build-arg PORT=8086 -t $REPO/notification-service:first .
docker build --platform linux/amd64 --build-arg SERVICE=api-gateway --build-arg PORT=8080 -t $REPO/api-gateway:first .
docker push $REPO/ticketing-service:first
docker push $REPO/notification-service:first
docker push $REPO/api-gateway:first
```

## 8. The two backends, private

`MSYS2_ARG_CONV_EXCL="httpGet.path="` at the start stops Git Bash from turning `/actuator/...` into a Windows path. Keep it, even on a Mac (it does nothing there).

If your CloudAMQP virtual host is not your username, add `,CLOUDAMQP_VHOST=<your vhost>` at the end of `--set-env-vars` in both commands.

```bash
MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy notification-service --image $REPO/notification-service:first --service-account notification-run@$PROJECT_ID.iam.gserviceaccount.com --no-allow-unauthenticated --port 8086 --cpu 1 --memory 512Mi --no-cpu-throttling --min-instances 1 --max-instances 1 --set-env-vars SPRING_PROFILES_ACTIVE=cloud,MONGODB_HOST=$MONGODB_HOST,MONGODB_USERNAME=$MONGODB_USERNAME,CLOUDAMQP_HOST=$CLOUDAMQP_HOST,CLOUDAMQP_USERNAME=$CLOUDAMQP_USERNAME --set-secrets MONGODB_PASSWORD=mongodb-password:latest,CLOUDAMQP_PASSWORD=cloudamqp-password:latest --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8086,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

```bash
MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy ticketing-service --image $REPO/ticketing-service:first --service-account ticketing-run@$PROJECT_ID.iam.gserviceaccount.com --no-allow-unauthenticated --port 8085 --cpu 1 --memory 512Mi --min-instances 0 --max-instances 2 --set-env-vars SPRING_PROFILES_ACTIVE=cloud,MONGODB_HOST=$MONGODB_HOST,MONGODB_USERNAME=$MONGODB_USERNAME,CLOUDAMQP_HOST=$CLOUDAMQP_HOST,CLOUDAMQP_USERNAME=$CLOUDAMQP_USERNAME --set-secrets MONGODB_PASSWORD=mongodb-password:latest,CLOUDAMQP_PASSWORD=cloudamqp-password:latest --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8085,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

Check: without credentials 403, with your identity token 200.

```bash
export TICKETING_URL=$(gcloud run services describe ticketing-service --format='value(status.url)')
export NOTIFICATIONS_URL=$(gcloud run services describe notification-service --format='value(status.url)')
curl -s -o /dev/null -w "%{http_code}\n" $TICKETING_URL/events
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $(gcloud auth print-identity-token)" $TICKETING_URL/events
```

## 9. The gateway, the only public URL

The gateway may call the two backends:

```bash
gcloud run services add-iam-policy-binding ticketing-service --member="serviceAccount:gateway-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/run.invoker
gcloud run services add-iam-policy-binding notification-service --member="serviceAccount:gateway-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/run.invoker
```

```bash
MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy api-gateway --image $REPO/api-gateway:first --service-account gateway-run@$PROJECT_ID.iam.gserviceaccount.com --allow-unauthenticated --port 8080 --cpu 1 --memory 512Mi --min-instances 0 --max-instances 2 --set-env-vars GATEWAY_TICKETINGURI=$TICKETING_URL,GATEWAY_NOTIFICATIONSURI=$NOTIFICATIONS_URL,GATEWAY_CLOUDRUNAUTH_ENABLED=true --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8080,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

```bash
export GATEWAY_URL=$(gcloud run services describe api-gateway --format='value(status.url)')
curl -s -o /dev/null -w "%{http_code}\n" $GATEWAY_URL/events
```

`200` means the system works. A `403` in the first few minutes is normal: new permissions take a while to apply. Wait 5 minutes and try again before you change anything.

## 10. Let GitHub deploy without a key (Workload Identity Federation)

The pool and the provider. The provider trusts GitHub's tokens only from your repository.

```bash
export PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
export DEPLOYER=github-deployer@$PROJECT_ID.iam.gserviceaccount.com
gcloud iam workload-identity-pools create github --location=global --display-name="GitHub Actions"
gcloud iam workload-identity-pools providers create-oidc github-actions --location=global --workload-identity-pool=github --issuer-uri=https://token.actions.githubusercontent.com --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" --attribute-condition="assertion.repository=='$GITHUB_REPO' && assertion.ref=='refs/heads/main'"
```

The deployer account and only the roles deploying needs: push images, update Cloud Run services, run them as the three runtime accounts.

```bash
gcloud iam service-accounts create github-deployer --display-name="GitHub Actions deployer"
gcloud artifacts repositories add-iam-policy-binding ticketing --location=$REGION --member="serviceAccount:$DEPLOYER" --role=roles/artifactregistry.writer
gcloud projects add-iam-policy-binding $PROJECT_ID --member="serviceAccount:$DEPLOYER" --role=roles/run.developer --condition=None
gcloud iam service-accounts add-iam-policy-binding ticketing-run@$PROJECT_ID.iam.gserviceaccount.com --member="serviceAccount:$DEPLOYER" --role=roles/iam.serviceAccountUser
gcloud iam service-accounts add-iam-policy-binding notification-run@$PROJECT_ID.iam.gserviceaccount.com --member="serviceAccount:$DEPLOYER" --role=roles/iam.serviceAccountUser
gcloud iam service-accounts add-iam-policy-binding gateway-run@$PROJECT_ID.iam.gserviceaccount.com --member="serviceAccount:$DEPLOYER" --role=roles/iam.serviceAccountUser
```

Only your repository may act as the deployer, and the deployer has no keys:

```bash
gcloud iam service-accounts add-iam-policy-binding $DEPLOYER --role=roles/iam.workloadIdentityUser --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/$GITHUB_REPO"
gcloud iam service-accounts keys list --iam-account $DEPLOYER --managed-by user
```

The keys list must be empty, and it stays empty.

## 11. The repository's four variables

Variables, not secrets: they name things, they don't unlock anything.

```bash
gh variable set GCP_PROJECT_ID --body "$PROJECT_ID"
gh variable set GCP_REGION --body "$REGION"
gh variable set GCP_WIF_PROVIDER --body "projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/providers/github-actions"
gh variable set GCP_DEPLOYER --body "$DEPLOYER"
gh variable list
```

## 12. The workflow

If your repository already has the deploy job from Session 10 in `.github/workflows/build.yml`, skip to section 13. `deploy/github-actions.md` explains what each job does; `deploy/rollback.md` is how to go back to an older revision. Otherwise take the class's workflow (it runs Gradle, builds the images and deploys on `main`):

```bash
mkdir -p .github/workflows
curl -fsSo .github/workflows/build.yml https://raw.githubusercontent.com/markvaradi94/future-ready-ticketing-app/main/.github/workflows/build.yml
git diff --stat
```

It expects the same layout as the class project: the three services in `ticketing-service/`, `notification-service/`, `api-gateway/` and one `Dockerfile` at the root with `ARG SERVICE` and `ARG PORT`.

## 13. Push and watch it deploy

```bash
git add .github/workflows/build.yml
git commit -m "deploy from the pipeline"
git push
gh run watch
```

If there was nothing to commit, start a run by hand: `gh workflow run build.yml --ref main`, then `gh run watch`.

All jobs green, the last one `deploy`. Check that Cloud Run now runs the pipeline's image (the label is your commit's SHA):

```bash
gcloud run revisions list --service api-gateway --limit 1 --format="value(metadata.labels.commit)"
git rev-parse HEAD
```

The two lines must be the same.

## 14. The end-to-end check (what the instructor runs)

```bash
curl -s -X POST $GATEWAY_URL/events -H "Content-Type: application/json" -d '{"name":"Cloud Night","venueName":"BT Arena","city":"Cluj","startsAt":"2030-06-12T20:00:00","capacity":100}'
curl -s -X POST $GATEWAY_URL/events/<the id printed above>/bookings -H "Content-Type: application/json" -H "X-Correlation-Id: final-check-1" -d '{"customerEmail":"ana@mail.ro","seats":2}'
curl -s $GATEWAY_URL/notifications
echo $GATEWAY_URL
```

The notification for the booking is in the list. The last line is the URL you hand in.

## 15. Save money between now and the check

`notification-service` is the only service that costs money while nobody uses it. Turn it off when you stop for the day and back on before you test:

```bash
gcloud run services update notification-service --min-instances 0
gcloud run services update notification-service --min-instances 1
```

When the project has been checked, remove everything with `deploy/cleanup.md`.

---

## When something goes wrong

| What you see | Why | What to do |
| --- | --- | --- |
| `ERROR: ... argument --image: expected one argument` or an empty name in an error | A variable is empty: a new terminal | Run section 0 again (and `GITHUB_REPO`, `PROJECT_NUMBER`, `DEPLOYER`, the URLs if you need them) |
| The project id is "already in use" | Someone in the world has it | Pick another `PROJECT_ID` and run section 0 again |
| The startup probe fails, the log shows `C:/Program Files/Git/actuator/...` | Git Bash rewrote the path | Keep `MSYS2_ARG_CONV_EXCL="httpGet.path="` at the start of the deploy command |
| The service starts, then cannot reach MongoDB | Atlas does not allow Cloud Run in | Atlas → Network Access → add `0.0.0.0/0` |
| The gateway answers 403 for a few minutes | New IAM bindings are slow | Wait 5 minutes, try again |
| The `auth` step fails: `unauthorized_client` / `rejected by the attribute condition` | `GITHUB_REPO` is not exactly your repository, or the run is not on `main` | `gh repo view --json nameWithOwner`, then recreate the provider with the right name: `gcloud iam workload-identity-pools providers update-oidc github-actions --location=global --workload-identity-pool=github --attribute-condition="assertion.repository=='<owner/name>' && assertion.ref=='refs/heads/main'"` |
| The `auth` step fails: `Permission 'iam.serviceAccounts.getAccessToken' denied` | The `workloadIdentityUser` binding names another repository | Run the first command of section 10's last block again with the right `GITHUB_REPO` |
| The `deploy` job is skipped | The variables are missing, or the push was not on `main` | `gh variable list`; push to `main` |
| The push of an image fails with `denied` | The deployer cannot write to the repository | Run the `artifactregistry.writer` line of section 10 again |
