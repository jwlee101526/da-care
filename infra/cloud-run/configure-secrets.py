import getpass
import json
import secrets
import subprocess
import sys
from urllib.parse import unquote, urlparse


def configure(project, replace=False):
    command = ["gcloud", "--project", project, "secrets", "versions"]
    existing = subprocess.run(
        command + ["list", "dacare-app-config", "--filter=state:ENABLED", "--format=value(name)", "--limit=1"],
        check=True, capture_output=True, text=True,
    ).stdout.strip()
    if existing and not replace:
        print("기존 앱 설정을 유지합니다.")
        return

    uri = urlparse(getpass.getpass("Neon 직접 연결 URI (postgresql://..., 입력 숨김): "))
    if uri.scheme not in ("postgresql", "postgres") or not uri.hostname or not uri.hostname.endswith(".neon.tech"):
        raise SystemExit("Neon의 직접 연결 URI를 입력하세요.")
    if "-pooler" in uri.hostname:
        raise SystemExit("Neon Connect에서 Connection pooling을 끄고 직접 연결 URI를 복사하세요.")
    if not uri.username or not uri.password or not uri.path.strip("/"):
        raise SystemExit("사용자, 비밀번호, DB 이름이 포함된 연결 URI가 필요합니다.")
    email = input("앱 관리자 이메일: ").strip()
    password = getpass.getpass("앱 관리자 비밀번호 (12자 이상): ")
    if "@" not in email or len(password) < 12:
        raise SystemExit("관리자 이메일과 12자 이상의 비밀번호가 필요합니다.")
    if password != getpass.getpass("관리자 비밀번호 확인: "):
        raise SystemExit("비밀번호가 일치하지 않습니다.")
    api_key = getpass.getpass("OpenAI API 키: ").strip()
    if not api_key:
        raise SystemExit("OpenAI API 키가 필요합니다.")
    config = {
        "DB_URL": f"jdbc:postgresql://{uri.hostname}:{uri.port or 5432}{uri.path}?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory",
        "DB_USERNAME": unquote(uri.username),
        "DB_PASSWORD": unquote(uri.password),
        "OPENAI_API_KEY": api_key,
        "JWT_SECRET": secrets.token_urlsafe(48),
        "ADMIN_EMAIL": email,
        "ADMIN_PASSWORD": password,
    }
    subprocess.run(command + ["add", "dacare-app-config", "--data-file=-"],
                   input=json.dumps(config), text=True, check=True, stdout=subprocess.DEVNULL)
    print("앱 설정을 Secret Manager에 저장했습니다.")


if __name__ == "__main__":
    configure(sys.argv[1], "--replace" in sys.argv[2:])
