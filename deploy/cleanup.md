# Removing everything from Google Cloud

Every command, in order, that removes what `deploy/setup-from-zero.md` (or Sessions 9 and 10) created. Do this **after your final project has been checked**: once it runs, your gateway URL is gone.

**How to use it**

- Run everything in **Git Bash**, from the root of your repository.
- One command per line. Paste one line, press Enter, read what it printed, then the next one.
- `NOT_FOUND` or "does not exist" means that thing is already gone: carry on.
- Deleting cannot be undone. Read the project id twice before you start.

There are two ways:

- **A. Delete the whole project.** One command removes everything inside it, and the billing stops. The simplest, and the one to choose unless you want to keep the project.
- **B. Delete piece by piece.** The project stays, empty, for something else.

Do section 1 first either way.

---

## 1. Stop the pipeline from deploying (both ways)

Without the four variables the `deploy` job is skipped, so a later push stays green instead of failing.

```bash
gh auth status
gh variable delete GCP_PROJECT_ID
gh variable delete GCP_REGION
gh variable delete GCP_WIF_PROVIDER
gh variable delete GCP_DEPLOYER
gh variable list
```

`gh variable list` prints nothing.

## 2. Make sure you are on the right project (both ways)

```bash
gcloud config configurations list
gcloud config configurations activate final-project
export PROJECT_ID=<your-project-id>
export REGION=europe-west1
gcloud config set project $PROJECT_ID
gcloud run services list
```

`gcloud run services list` must show **your** three services. If it shows something else, stop and check the project id.

---

## A. Delete the whole project

```bash
gcloud projects delete $PROJECT_ID
```

It asks you to confirm: type `Y`. Then check:

```bash
gcloud projects describe $PROJECT_ID --format="value(lifecycleState)"
```

It prints `DELETE_REQUESTED`. Everything in the project stops at once and nothing more is billed. Google keeps the project for 30 days, and `gcloud projects undelete $PROJECT_ID` brings it back during that time; after that it is gone for good. Until then it still counts toward the number of projects your account may have.

Go to section 3.

---

## B. Delete piece by piece

### B1. The three services

```bash
gcloud run services delete api-gateway --quiet
gcloud run services delete ticketing-service --quiet
gcloud run services delete notification-service --quiet
```

### B2. The image repository, with every image in it

```bash
gcloud artifacts repositories delete ticketing --location=$REGION --quiet
```

### B3. The two secrets

```bash
gcloud secrets delete mongodb-password --quiet
gcloud secrets delete cloudamqp-password --quiet
```

### B4. GitHub's access: the provider and the pool

```bash
gcloud iam workload-identity-pools providers delete github-actions --location=global --workload-identity-pool=github --quiet
gcloud iam workload-identity-pools delete github --location=global --quiet
```

The name `github` stays reserved for 30 days: to set the pipeline up again in this project sooner, use `gcloud iam workload-identity-pools undelete github --location=global` instead of creating it.

### B5. The four service accounts

```bash
gcloud iam service-accounts delete github-deployer@$PROJECT_ID.iam.gserviceaccount.com --quiet
gcloud iam service-accounts delete gateway-run@$PROJECT_ID.iam.gserviceaccount.com --quiet
gcloud iam service-accounts delete ticketing-run@$PROJECT_ID.iam.gserviceaccount.com --quiet
gcloud iam service-accounts delete notification-run@$PROJECT_ID.iam.gserviceaccount.com --quiet
```

### B6. The deployer's role on the project

Deleting the account leaves its project role behind as `deleted:serviceAccount:...`. It grants nothing, but remove it so the project is clean:

```bash
gcloud projects get-iam-policy $PROJECT_ID --flatten="bindings[].members" --filter="bindings.members:github-deployer" --format="value(bindings.role,bindings.members)"
gcloud projects remove-iam-policy-binding $PROJECT_ID --role=roles/run.developer --member="<the member printed above, starting with deleted:serviceAccount:>" --condition=None
```

If the first command prints nothing, skip the second.

### B7. Check that it is empty

Each command prints nothing, or only Google's own entries (`Listed 0 items.`):

```bash
gcloud run services list
gcloud artifacts repositories list
gcloud secrets list
gcloud iam workload-identity-pools list --location=global
gcloud iam service-accounts list --filter="email~-run@ OR email~github-deployer"
```

### B8. Stop billing on the empty project (optional)

```bash
gcloud billing projects unlink $PROJECT_ID
```

Without billing, the project can't run anything that costs money. Link it again (section 2 of `deploy/setup-from-zero.md`) when you need it.

---

## 3. Your laptop (both ways)

The images you built for the first deploy:

```bash
docker image ls --filter "reference=*-docker.pkg.dev/*/ticketing/*"
docker image rm $REGION-docker.pkg.dev/$PROJECT_ID/ticketing/ticketing-service:first $REGION-docker.pkg.dev/$PROJECT_ID/ticketing/notification-service:first $REGION-docker.pkg.dev/$PROJECT_ID/ticketing/api-gateway:first
```

The `gcloud` configuration, and the passwords in this terminal:

```bash
gcloud config configurations activate default
gcloud config configurations delete final-project --quiet
unset MONGODB_PASSWORD CLOUDAMQP_PASSWORD
```

## 4. What is not in Google Cloud

- **The budget alert** belongs to your billing account, not the project. It costs nothing; keep it, or delete it in the console: Billing → **Budgets & alerts**.
- **Atlas and CloudAMQP** are free tiers and outside Google Cloud. Keep them, or delete the cluster and the instance in their own consoles.
- **Your GitHub repository** stays as it is: the code, the workflow and its run history are your portfolio.

## 5. A week later: is anything still billed?

Console → search `Billing` → **Reports** → filter on your project. The cost line stops at the day you cleaned up.
