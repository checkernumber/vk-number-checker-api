// Browser code must call a same-origin backend proxy; never expose an API key.
// The proxy is responsible for submitting the task, polling for completion,
// and returning (or downloading) the result server-side.

async function checkFile(file) {
  const form = new FormData();
  form.append("file", file); // a File/Blob, e.g. from <input type="file">
  form.append("task_type", "vk");

  const response = await fetch("/api/async-check-proxy", {
    method: "POST",
    body: form,
  });
  if (!response.ok) {
    throw new Error(`request failed: ${response.status}`);
  }
  return response.json();
}

// Example wiring:
// document.querySelector("#file").addEventListener("change", async (e) => {
//   const result = await checkFile(e.target.files[0]);
//   console.log(result);
// });
