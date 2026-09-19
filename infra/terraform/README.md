# OCI Terraform

환경별로 독립 state를 사용한다. state 버킷은 Terraform 실행 전에 생성하고 공개 접근을 금지한다.

```powershell
terraform init -backend-config=backend.hcl
terraform plan -var-file=staging.tfvars
```

`backend.hcl`, `*.tfvars`, OCI API private key는 저장소에 추가하지 않는다. Terraform은 Vault Secret 컨테이너가 아닌 기존 Secret OCID만 VM에 전달하므로 비밀 값이 state에 남지 않는다.

배포 전 DNS A 레코드를 `public_ip` 출력값으로 설정한 뒤 VM의 Vault secret dotenv에 `APP_DOMAIN`과 `OCI_BACKUP_BUCKET`을 입력한다.
