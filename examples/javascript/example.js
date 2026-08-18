// Browser code must call a same-origin backend proxy; never expose an API key.
const form=new FormData();form.append("file",new File(["PLACEHOLDER_IDENTIFIER\n"],"numbers.txt"));form.append("task_type","vk");fetch("/api/async-check-proxy",{method:"POST",body:form}).then(r=>r.json()).then(console.log); // proxy submits, polls, and downloads
