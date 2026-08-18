import os
import time

import requests

API_KEY = os.environ.get("CHECKNUMBER_API_KEY")
if not API_KEY:
    raise SystemExit("Set the CHECKNUMBER_API_KEY environment variable")

BASE_URL = "https://api.checknumber.ai"
HEADERS = {"X-API-Key": API_KEY}
INPUT_FILE = os.path.join(os.path.dirname(__file__), "../numbers.txt")

with open(INPUT_FILE, "rb") as fh:
    resp = requests.post(
        f"{BASE_URL}/v1/tasks",
        headers=HEADERS,
        files={"file": fh},
        data={"task_type": "vk"},
        timeout=60,
    )
resp.raise_for_status()
task = resp.json()
print(f"submitted task {task['task_id']} status={task['status']}")

while task.get("status") not in ("exported", "failed"):
    time.sleep(5)
    resp = requests.post(
        f"{BASE_URL}/v1/gettasks",
        headers=HEADERS,
        data={"task_id": task["task_id"]},
        timeout=60,
    )
    resp.raise_for_status()
    task = resp.json()
    print(f"status={task['status']}")

if task["status"] == "failed":
    raise SystemExit(f"task failed: {task}")

result_url = task.get("result_url")
if not result_url:
    raise SystemExit(f"task exported but no result_url: {task}")

download = requests.get(result_url, timeout=120)
download.raise_for_status()
with open("results.zip", "wb") as out:
    out.write(download.content)
print("saved results.zip")
