#!/usr/bin/env bash
#
# Session 10: lets GitHub Actions deploy to your Google Cloud project without any key.
#
# Read it before you run it. It is the four set-up sections of deploy/github-actions.md in one file.
#
# Run it once, in Git Bash, from the repository root, after `gcloud auth login`:
#
#   PROJECT_ID=your-project-id GITHUB_REPO=your-github-user/your-repository bash deploy/setup-pipeline.sh
#
# It expects what Session 9 created in the project: the "ticketing" image repository and the three
# runtime service accounts (ticketing-run, notification-run, gateway-run).

set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID to your Google Cloud project id}"
: "${GITHUB_REPO:?Set GITHUB_REPO to owner/name, exactly as on GitHub}"

REGION="${REGION:-europe-west1}"
PROVIDER="${PROVIDER:-github-actions}"
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
DEPLOYER="github-deployer@$PROJECT_ID.iam.gserviceaccount.com"

gcloud config set project "$PROJECT_ID"

echo
echo "== 1. The pool and the provider =="
# The provider trusts GitHub's tokens, but only from your repository's main branch (the attribute condition).
# Without the condition, any GitHub repository could try to use your pool.
gcloud iam workload-identity-pools create github --location=global --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc "$PROVIDER" \
    --location=global --workload-identity-pool=github \
    --issuer-uri=https://token.actions.githubusercontent.com \
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
    --attribute-condition="assertion.repository=='$GITHUB_REPO' && assertion.ref=='refs/heads/main'"

echo
echo "== 2. The deployer account and its three roles =="
# Only what deploying needs: push images to this one repository, update Cloud Run services,
# and run them as the three runtime accounts.
gcloud iam service-accounts create github-deployer --display-name="GitHub Actions deployer"

gcloud artifacts repositories add-iam-policy-binding ticketing --location="$REGION" \
    --member="serviceAccount:$DEPLOYER" --role=roles/artifactregistry.writer

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$DEPLOYER" --role=roles/run.developer --condition=None

for sa in ticketing-run notification-run gateway-run; do
  gcloud iam service-accounts add-iam-policy-binding "$sa@$PROJECT_ID.iam.gserviceaccount.com" \
      --member="serviceAccount:$DEPLOYER" --role=roles/iam.serviceAccountUser
done

echo
echo "== 3. Only your repository may act as the deployer =="
gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER" \
    --role=roles/iam.workloadIdentityUser \
    --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/$GITHUB_REPO"

echo
echo "The deployer's keys (this list must be empty, and it stays empty):"
gcloud iam service-accounts keys list --iam-account "$DEPLOYER" --managed-by user

echo
echo "== 4. The repository's variables =="
# Four identifiers, not secrets: they name things, they don't unlock them.
WIF_PROVIDER="projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/providers/$PROVIDER"

if command -v gh >/dev/null 2>&1; then
  gh variable set GCP_PROJECT_ID   --body "$PROJECT_ID"   --repo "$GITHUB_REPO"
  gh variable set GCP_REGION       --body "$REGION"       --repo "$GITHUB_REPO"
  gh variable set GCP_WIF_PROVIDER --body "$WIF_PROVIDER" --repo "$GITHUB_REPO"
  gh variable set GCP_DEPLOYER     --body "$DEPLOYER"     --repo "$GITHUB_REPO"
  echo "Set four variables on $GITHUB_REPO."
else
  echo "The GitHub CLI (gh) is not installed. Add these four variables by hand in GitHub:"
  echo "your repository -> Settings -> Secrets and variables -> Actions -> Variables"
  echo
  echo "  GCP_PROJECT_ID   = $PROJECT_ID"
  echo "  GCP_REGION       = $REGION"
  echo "  GCP_WIF_PROVIDER = $WIF_PROVIDER"
  echo "  GCP_DEPLOYER     = $DEPLOYER"
fi

echo
echo "Done. Next: add the deploy job to .github/workflows/build.yml and push to main."
