const fs = require("fs");
const path = require("path");

const API_KEY = process.env.CHECKNUMBER_API_KEY;
if (!API_KEY) {
  throw new Error("Set the CHECKNUMBER_API_KEY environment variable");
}

const BASE_URL = "https://api.checknumber.ai";
const INPUT_FILE = path.join(__dirname, "../numbers.txt");

async function pollUntilDone(taskId) {
  for (;;) {
    const res = await fetch(`${BASE_URL}/v1/gettasks`, {
      method: "POST",
      headers: { "X-API-Key": API_KEY, "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ task_id: taskId }),
    });
    if (!res.ok) {
      throw new Error(`gettasks failed: ${res.status} ${await res.text()}`);
    }
    const task = await res.json();
    console.log(`status=${task.status}`);
    if (task.status === "exported" || task.status === "failed") return task;
    await new Promise((r) => setTimeout(r, 5000));
  }
}

async function main() {
  const form = new FormData();
  form.append("file", new Blob([fs.readFileSync(INPUT_FILE)]), "numbers.txt");
  form.append("task_type", "vk");

  const submitRes = await fetch(`${BASE_URL}/v1/tasks`, {
    method: "POST",
    headers: { "X-API-Key": API_KEY },
    body: form,
  });
  if (!submitRes.ok) {
    throw new Error(`submit failed: ${submitRes.status} ${await submitRes.text()}`);
  }
  let task = await submitRes.json();
  console.log(`submitted task ${task.task_id} status=${task.status}`);

  if (task.status !== "exported" && task.status !== "failed") {
    task = await pollUntilDone(task.task_id);
  }

  if (task.status === "failed") {
    throw new Error(`task failed: ${JSON.stringify(task)}`);
  }
  if (!task.result_url) {
    throw new Error(`task exported but no result_url: ${JSON.stringify(task)}`);
  }

  const download = await fetch(task.result_url);
  if (!download.ok) {
    throw new Error(`download failed: ${download.status}`);
  }
  fs.writeFileSync("results.zip", Buffer.from(await download.arrayBuffer()));
  console.log("saved results.zip");
}

main().catch((err) => {
  console.error(err.message);
  process.exit(1);
});
