# 단일 OCI 운영 VM

Terraform은 운영 VM, 네트워크, 22·80·443 보안 규칙, 고정 공인 IP만 관리한다. 애플리케이션 배포는 GitHub Actions가 담당한다.

Terraform state bucket은 최초 한 번 OCI Console에서 생성해야 한다. 실제 자격증명은 GitHub `production` Environment에만 저장한다.
