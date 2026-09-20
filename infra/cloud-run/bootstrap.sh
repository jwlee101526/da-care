#!/usr/bin/env bash
set -euo pipefail

PROJECT=${1:?사용법: bash infra/cloud-run/bootstrap.sh 프로젝트ID 소유자/저장소}
REPOSITORY=${2:?GitHub 소유자/저장소가 필요합니다}
REGION=${GCP_REGION:-asia-northeast3}
[[ "$PROJECT" =~ ^[a-z][a-z0-9-]{4,28}[a-z0-9]$ ]] || exit 1
[[ "$REPOSITORY" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]] || exit 1
command -v gcloud >/dev/null
command -v python3 >/dev/null
if ! command -v gh >/dev/null; then
  sudo apt-get update
  sudo apt-get install -y gh
fi
gh auth status >/dev/null 2>&1 || gh auth login --hostname github.com --git-protocol https --web
REPOSITORY_ID=$(gh api "repos/$REPOSITORY" --jq .id)
OWNER_ID=$(gh api "repos/$REPOSITORY" --jq .owner.id)
gcloud config set project "$PROJECT"
[[ $(gcloud billing projects describe "$PROJECT" --format='value(billingEnabled)') == True ]] || {
  echo '프로젝트에 결제 계정을 먼저 연결하세요.' >&2
  exit 1
}
gcloud services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com storage.googleapis.com cloudresourcemanager.googleapis.com --project="$PROJECT"
NUMBER=$(gcloud projects describe "$PROJECT" --format='value(projectNumber)')
BUCKET="$PROJECT-dacare-tfstate"
DEPLOY="dacare-deploy@$PROJECT.iam.gserviceaccount.com"
RUNTIME="dacare-runtime@$PROJECT.iam.gserviceaccount.com"

if ! gcloud storage buckets describe "gs://$BUCKET" >/dev/null 2>&1; then
  gcloud storage buckets create "gs://$BUCKET" --project="$PROJECT" --location="$REGION" --uniform-bucket-level-access --public-access-prevention
fi
gcloud storage buckets update "gs://$BUCKET" --versioning
for account in dacare-deploy dacare-runtime; do
  if ! gcloud iam service-accounts describe "$account@$PROJECT.iam.gserviceaccount.com" >/dev/null 2>&1; then
    gcloud iam service-accounts create "$account" --project="$PROJECT"
  fi
done
if ! gcloud artifacts repositories describe dacare --location="$REGION" >/dev/null 2>&1; then
  gcloud artifacts repositories create dacare --repository-format=docker --location="$REGION"
fi
gcloud artifacts repositories add-iam-policy-binding dacare --location="$REGION" --member="serviceAccount:$DEPLOY" --role=roles/artifactregistry.writer --quiet >/dev/null
gcloud storage buckets add-iam-policy-binding "gs://$BUCKET" --member="serviceAccount:$DEPLOY" --role=roles/storage.objectAdmin --quiet >/dev/null
gcloud projects add-iam-policy-binding "$PROJECT" --member="serviceAccount:$DEPLOY" --role=roles/run.admin --condition=None --quiet >/dev/null
gcloud projects add-iam-policy-binding "$PROJECT" --member="serviceAccount:$DEPLOY" --role=roles/serviceusage.serviceUsageConsumer --condition=None --quiet >/dev/null
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME" --member="serviceAccount:$DEPLOY" --role=roles/iam.serviceAccountUser --quiet >/dev/null

if ! gcloud iam workload-identity-pools describe dacare-github --location=global >/dev/null 2>&1; then
  gcloud iam workload-identity-pools create dacare-github --location=global --display-name='다케어 GitHub'
fi
PROVIDER="projects/$NUMBER/locations/global/workloadIdentityPools/dacare-github/providers/github"
CONDITION="assertion.repository_id == '$REPOSITORY_ID' && assertion.repository_owner_id == '$OWNER_ID' && assertion.ref == 'refs/heads/production'"
MAPPING='google.subject=assertion.sub,attribute.repository_id=assertion.repository_id'
if gcloud iam workload-identity-pools providers describe github --workload-identity-pool=dacare-github --location=global >/dev/null 2>&1; then
  gcloud iam workload-identity-pools providers update-oidc github --workload-identity-pool=dacare-github --location=global --attribute-mapping="$MAPPING" --attribute-condition="$CONDITION"
else
  gcloud iam workload-identity-pools providers create-oidc github --workload-identity-pool=dacare-github --location=global --issuer-uri=https://token.actions.githubusercontent.com --attribute-mapping="$MAPPING" --attribute-condition="$CONDITION"
fi
gcloud iam service-accounts add-iam-policy-binding "$DEPLOY" --role=roles/iam.workloadIdentityUser --member="principalSet://iam.googleapis.com/projects/$NUMBER/locations/global/workloadIdentityPools/dacare-github/attribute.repository_id/$REPOSITORY_ID" --quiet >/dev/null

if ! gcloud secrets describe dacare-app-config >/dev/null 2>&1; then
  gcloud secrets create dacare-app-config --replication-policy=automatic
fi
gcloud secrets add-iam-policy-binding dacare-app-config --member="serviceAccount:$RUNTIME" --role=roles/secretmanager.secretAccessor --quiet >/dev/null
python3 "$(dirname "$0")/configure-secrets.py" "$PROJECT"

gh variable set GCP_PROJECT_ID --repo "$REPOSITORY" --body "$PROJECT"
gh variable set GCP_REGION --repo "$REPOSITORY" --body "$REGION"
gh variable set GCP_WIF_PROVIDER --repo "$REPOSITORY" --body "$PROVIDER"
printf '\n초기 설정 완료. production 브랜치에 push하면 배포됩니다.\n'
