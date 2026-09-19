# 단일 운영 VM 배포

OCI VM 한 대에서 Docker Compose를 실행한다. `production` 브랜치 push 시 GitHub Actions가 테스트, 이미지 발행, SSH 배포를 순서대로 수행한다.

GitHub `production` Environment Secret `APP_ENV`에는 아래 dotenv 전체를 저장한다.

```dotenv
APP_DOMAIN=example.com
GHCR_USERNAME=github-user
GHCR_TOKEN=github_pat_read_packages_only
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
CORS_ALLOWED_ORIGINS=https://example.com
```

VM에는 Ubuntu ARM 이미지와 SSH public key만 설정한다. Docker와 운영 파일은 첫 배포 때 GitHub Actions가 설치한다. DB는 Docker volume에만 저장하며 외부 포트를 열지 않는다.
