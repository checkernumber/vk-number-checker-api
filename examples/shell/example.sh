#!/usr/bin/env bash
# Requires: curl, jq
set -euo pipefail
: "${CHECKNUMBER_API_KEY:?Set the CHECKNUMBER_API_KEY environment variable}"

BASE_URL="https://api.checknumber.ai"
DIR="$(cd "$(dirname "$0")" && pwd)"

task=$(curl --fail-with-body -sS -X POST "$BASE_URL/v1/tasks" \
  -H "X-API-Key: ${CHECKNUMBER_API_KEY}" \
  -F "file=@${DIR}/../numbers.txt" \
  -F "task_type=vk")
task_id=$(echo "$task" | jq -r ".task_id")
status=$(echo "$task" | jq -r ".status")
echo "submitted task ${task_id} status=${status}"

while [[ "$status" != "exported" && "$status" != "failed" ]]; do
  sleep 5
  task=$(curl --fail-with-body -sS -X POST "$BASE_URL/v1/gettasks" \
    -H "X-API-Key: ${CHECKNUMBER_API_KEY}" \
    -d "task_id=${task_id}")
  status=$(echo "$task" | jq -r ".status")
  echo "status=${status}"
done

if [[ "$status" == "failed" ]]; then
  echo "task failed: $task" >&2
  exit 1
fi

result_url=$(echo "$task" | jq -r ".result_url // empty")
if [[ -z "$result_url" ]]; then
  echo "task exported but no result_url: $task" >&2
  exit 1
fi

curl --fail-with-body -sS -o results.zip "$result_url"
echo "saved results.zip"
