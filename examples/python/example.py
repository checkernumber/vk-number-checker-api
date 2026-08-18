import os, time, requests
key=os.environ.get("CHECKNUMBER_API_KEY")
if not key: raise SystemExit("Set CHECKNUMBER_API_KEY")
base="https://api.checknumber.ai"
with open(os.path.join(os.path.dirname(__file__), "../numbers.txt"), "rb") as fh:
    r=requests.post(base+"/v1/tasks", headers={"X-API-Key":key}, files={"file":fh}, data={"task_type":"vk"}, timeout=60)
r.raise_for_status(); task=r.json(); print(task)
while task.get("status") not in ("exported", "failed"):
    time.sleep(5); task=requests.post(base+"/v1/gettasks",headers={"X-API-Key":key},data={"task_id":task["task_id"]},timeout=60).json(); print(task.get("status"))
if task.get("result_url"): open("results.zip","wb").write(requests.get(task["result_url"],timeout=120).content)
