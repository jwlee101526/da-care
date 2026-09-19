# 운영 VM 런타임

Terraform cloud-init이 이 디렉터리의 파일을 `/opt/dacare/runtime`과 `/opt/dacare/bin`에 설치한다.

Vault의 단일 Secret은 dotenv 형식이며 다음 값을 포함한다.

```dotenv
APP_DOMAIN=staging.example.com
GHCR_USERNAME=github-user
GHCR_TOKEN=ghp_read_packages_only
MARIADB_DATABASE=dacare
MARIADB_USER=dacare
MARIADB_PASSWORD=replace-me
MARIADB_ROOT_PASSWORD=replace-me
DB_URL=jdbc:mariadb://mariadb:3306/dacare
DB_USERNAME=dacare
DB_PASSWORD=replace-me
JWT_SECRET=at-least-32-byte-random-value
JWT_EXPIRATION=PT8H
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=replace-me
OPENAI_API_KEY=replace-me
SLACK_WEBHOOK_URL=
SOLAPI_API_KEY=
SOLAPI_API_SECRET=
SOLAPI_SENDER=
SOLAPI_KAKAO_PF_ID=
SOLAPI_KAKAO_TEMPLATE_ID=
CORS_ALLOWED_ORIGINS=https://staging.example.com
OCI_BACKUP_BUCKET=dacare-staging-backups
```

Secret 값은 Terraform, Git 저장소, Actions 로그에 넣지 않는다. VM은 Instance Principal로만 Secret bundle과 백업 버킷에 접근한다.
