#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

: "${TF_RECOVERY_IMPORTS:?복구할 리소스의 주소와 OCID JSON이 필요합니다}"
: "${RUNNER_TEMP:?RUNNER_TEMP가 필요합니다}"

printf '%s' "$TF_RECOVERY_IMPORTS" | jq -e '
  type == "object" and length > 0 and
  all(to_entries[];
    (.key | test("^oci_core_(vcn|internet_gateway|security_list|route_table|subnet)\\.production$")) and
    (.value | type == "string" and test("^ocid1\\.[a-z]+\\.[a-z0-9-]+\\.[a-z0-9-]+\\.[a-z0-9]+$")))
' >/dev/null

snapshot="$RUNNER_TEMP/state-before-recovery.tfstate"
current="$RUNNER_TEMP/state-current-recovery.tfstate"
error_file="$RUNNER_TEMP/state-recovery-error.txt"

pull_state() {
  if terraform state pull > "$current" 2> "$error_file"; then
    if [[ ! -s "$current" ]]; then
      printf '{"version":4,"resources":[]}\n' > "$current"
    fi
    jq -e '.version == 4 and (.resources | type == "array")' "$current" >/dev/null
  elif grep -q 'No state file was found' "$error_file"; then
    printf '{"version":4,"resources":[]}\n' > "$current"
  else
    cat "$error_file" >&2
    return 1
  fi
}

pull_state
cp "$current" "$snapshot"
while IFS=$'\t' read -r address resource_id; do
  existing_id=$(jq -r --arg address "$address" '
    [.resources[] | select(.mode == "managed" and (.module // "") == "") |
      select((.type + "." + .name) == $address) | .instances[].attributes.id] |
    if length == 0 then "" elif length == 1 then .[0] else error("복수 인스턴스는 자동 복구하지 않습니다") end
  ' "$current")
  if [[ -n "$existing_id" ]]; then
    if [[ "$existing_id" != "$resource_id" ]]; then
      echo "::error::$address 가 다른 OCID로 등록돼 있습니다. 덮어쓰지 않습니다." >&2
      exit 1
    fi
    echo "이미 복구됨: $address"
    continue
  fi
  terraform import -input=false "$address" "$resource_id"
  pull_state
done < <(printf '%s' "$TF_RECOVERY_IMPORTS" | jq -r 'to_entries[] | [.key, .value] | @tsv')

echo '네트워크 리소스 state 복구 완료. VM 생성/apply는 실행하지 않았습니다.'
