# VK Number Checker API — official examples | CheckNumber

This repository documents the CheckNumber VK Number Checker API integration contract using facts from the expansion manifest.

- Product page: https://checknumber.ai/products/vk
- API base URL: `https://api.checknumber.ai`
- Authentication: `X-API-Key`
- API key environment variable: `CHECKNUMBER_API_KEY`
- License: MIT

## Processing model

This is a file-based asynchronous API. Submit an authorized text file, poll the task until `exported`, then download the result URL. It is not a realtime single-identifier endpoint.

```bash
curl -X POST "https://api.checknumber.ai/v1/tasks" -H "X-API-Key: ${CHECKNUMBER_API_KEY}" -F "file=@examples/numbers.txt" -F "task_type=vk"
curl -X POST "https://api.checknumber.ai/v1/gettasks" -H "X-API-Key: ${CHECKNUMBER_API_KEY}" -F "task_id=YOUR_TASK_ID"
```

## Products and limits

| Code | Name | Input | Limits | Result fields | Documentation |
| --- | --- | --- | --- | --- | --- |
| `vk` | Number Checker | phone | 1000–10000000 rows | `number, activated` | [docs](https://docs.checknumber.ai/vk-checker) |

## Response and boundaries

- Treat only an explicit determined result as a positive or negative classification; `undetermined`/`exists=false` is not a negative.
- Authentication keys must come from environment variables or a secret manager; never commit keys or call private/internal endpoints.
- Product prices can change. Check the official pricing page at runtime; this repository intentionally does not hard-code a price.
- Use only identifiers you are authorized to process and follow applicable privacy, platform, and data-protection requirements.

## Runnable examples

Eight self-contained examples are provided under `examples/`: Python, Node.js, Go, Java, C#, PHP, and Shell each implement the full submit → poll → download workflow with HTTP error handling; browser JavaScript submits through a same-origin proxy contract and never contains an API key. Set the server-side API-key environment variable before running them.

## Official resources

- `vk`: https://docs.checknumber.ai/vk-checker
- Pricing: https://checknumber.ai/pricing
- OpenAPI contract: [openapi.yaml](openapi.yaml)

Last reviewed: 2026-08-18.
