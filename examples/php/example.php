<?php
$apiKey = getenv("CHECKNUMBER_API_KEY");
if (!$apiKey) {
    fwrite(STDERR, "Set the CHECKNUMBER_API_KEY environment variable\n");
    exit(1);
}

$baseUrl = "https://api.checknumber.ai";

function request(string $url, array $headers, array $postFields = null): array {
    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => $headers,
    ]);
    if ($postFields !== null) {
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postFields);
    }
    $body = curl_exec($ch);
    if ($body === false) {
        fwrite(STDERR, "request error: " . curl_error($ch) . "\n");
        exit(1);
    }
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    if ($status >= 300) {
        fwrite(STDERR, "request failed: $status $body\n");
        exit(1);
    }
    return json_decode($body, true);
}

$task = request("$baseUrl/v1/tasks", ["X-API-Key: $apiKey"], [
    "task_type" => "vk",
    "file" => new CURLFile(__DIR__ . "/../numbers.txt"),
]);
echo "submitted task {$task['task_id']} status={$task['status']}\n";

while (!in_array($task["status"], ["exported", "failed"], true)) {
    sleep(5);
    $task = request("$baseUrl/v1/gettasks", ["X-API-Key: $apiKey"], ["task_id" => $task["task_id"]]);
    echo "status={$task['status']}\n";
}

if ($task["status"] === "failed") {
    fwrite(STDERR, "task failed: " . json_encode($task) . "\n");
    exit(1);
}
if (empty($task["result_url"])) {
    fwrite(STDERR, "task exported but no result_url: " . json_encode($task) . "\n");
    exit(1);
}

$ch = curl_init($task["result_url"]);
curl_setopt_array($ch, [CURLOPT_RETURNTRANSFER => true]);
$data = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);
if ($data === false || $status >= 300) {
    fwrite(STDERR, "download failed: $status\n");
    exit(1);
}
file_put_contents("results.zip", $data);
echo "saved results.zip\n";
