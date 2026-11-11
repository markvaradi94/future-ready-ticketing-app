# Deploying from GitHub Actions

How a push to `main` deploys the three services, with no service-account key anywhere. GitHub Actions gets a
short-lived token for each run, signed by GitHub, and Google exchanges it for an hour of a deployer account's
permissions, but only for this repository: that is Workload Identity Federation. Run the set-up once, after
`deploy/cloud-run.md`; the pipeline itself is `.github/workflows/build.yml`.

## The set-up, once per project

`deploy/setup-pipeline.sh` runs the four steps below. Read it, then run it in Git Bash from the repository root:

    PROJECT_ID=your-project-id GITHUB_REPO=your-github-user/your-repository bash deploy/setup-pipeline.sh

`GITHUB_REPO` is `owner/name`, exactly as on GitHub: the provider compares it letter by letter.

1. **The pool and the provider.** The pool `github` holds outside identities; the provider `github-actions` trusts
   tokens signed by GitHub (`https://token.actions.githubusercontent.com`), but only from your repository's `main`:
   `--attribute-condition="assertion.repository=='owner/name' && assertion.ref=='refs/heads/main'"`. Without the
   repository, any GitHub repository could try to use your pool; without the branch, a workflow changed on any other
   branch could deploy, whatever the deploy job's `if` says.
2. **The deployer and its three roles.** `github-deployer` may push images to the `ticketing` repository
   (`artifactregistry.writer`), update Cloud Run services (`run.developer`), and deploy them as the three runtime
   accounts (`iam.serviceAccountUser` on each). Nothing else: it cannot read the secrets itself or change who may call a service.
3. **Only your repository may act as the deployer** (`iam.workloadIdentityUser` for the repository's principal set).
   The deployer has no keys, and that is how it stays:

       gcloud iam service-accounts keys list --iam-account github-deployer@$PROJECT_ID.iam.gserviceaccount.com --managed-by user

4. **The repository's four variables:** `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_WIF_PROVIDER`, `GCP_DEPLOYER`.
   Variables, not secrets: they name things, they do not unlock them, and they are printed in the run logs, which
   are public on a public repository.

The pool's name stays reserved for 30 days after it is deleted: run the set-up once per project.

## The pipeline

`.github/workflows/build.yml` has four jobs, each one waiting for the one before:

- `compile` runs on every push and pull request: the Gradle build without the tests.
- `test` runs the Gradle build with all tests.
- `images` builds the three images with `docker compose build`, to prove the Dockerfile still works.
- `deploy` runs after `images` passes, only on `main` (a push, or started by hand on `main`), and only if the four variables are
  set (a fork, or a repository before the set-up, skips it and stays green). It:
  1. exchanges GitHub's token for the deployer's permissions;
  2. builds the three images tagged with the commit's SHA and pushes them;
  3. gives each Cloud Run service the new image and a `commit` label, and nothing else: the configuration from
     `deploy/cloud-run.md` stays;
  4. sends all traffic to the new revisions (`--to-latest`);
  5. smoke-tests the gateway's public URL.

  It deploys with no approval step: a green build on `main` goes straight to Cloud Run. One deploy runs at a time
  (`concurrency`): a newer run waits, and replaces an older one that is still waiting. To add an approval, create a
  GitHub environment `production` (Settings → Environments, yourself as required reviewer, `main` only) and add
  `environment: production` to the deploy job.

Only the deploy job may ask for GitHub's token (`id-token: write`); everything else reads the repository and
nothing more. A run takes about ten minutes.

To start a deploy by hand: GitHub → Actions → Build → Run workflow, or `gh workflow run build.yml --ref main`.
To go back to an older revision: `deploy/rollback.md`.
