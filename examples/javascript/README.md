# Browser JavaScript example

Browser code must never hold an API key. This example calls a same-origin
backend proxy endpoint (`/api/async-check-proxy`) that your server implements;
the proxy is responsible for submitting the task, polling for completion, and
returning (or downloading) the result server-side.
