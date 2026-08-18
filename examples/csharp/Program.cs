using System;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Threading.Tasks;

class Program
{
    const string BaseUrl = "https://api.checknumber.ai";

    static async Task<JsonElement> SendAsync(HttpClient client, HttpRequestMessage req)
    {
        var resp = await client.SendAsync(req);
        var body = await resp.Content.ReadAsStringAsync();
        if (!resp.IsSuccessStatusCode)
        {
            throw new Exception($"request failed: {(int)resp.StatusCode} {body}");
        }
        return JsonDocument.Parse(body).RootElement.Clone();
    }

    static async Task Main()
    {
        var apiKey = Environment.GetEnvironmentVariable("CHECKNUMBER_API_KEY");
        if (string.IsNullOrEmpty(apiKey))
        {
            throw new Exception("Set the CHECKNUMBER_API_KEY environment variable");
        }

        using var client = new HttpClient();
        client.DefaultRequestHeaders.Add("X-API-Key", apiKey);

        using var form = new MultipartFormDataContent();
        form.Add(new StringContent("vk"), "task_type");
        var fileContent = new StreamContent(File.OpenRead("../numbers.txt"));
        fileContent.Headers.ContentType = new MediaTypeHeaderValue("text/plain");
        form.Add(fileContent, "file", "numbers.txt");

        var submitReq = new HttpRequestMessage(HttpMethod.Post, $"{BaseUrl}/v1/tasks") { Content = form };
        var task = await SendAsync(client, submitReq);
        var taskId = task.GetProperty("task_id").GetString();
        var status = task.GetProperty("status").GetString();
        Console.WriteLine($"submitted task {taskId} status={status}");

        while (status != "exported" && status != "failed")
        {
            await Task.Delay(5000);
            var pollReq = new HttpRequestMessage(HttpMethod.Post, $"{BaseUrl}/v1/gettasks")
            {
                Content = new FormUrlEncodedContent(new[]
                {
                    new System.Collections.Generic.KeyValuePair<string, string>("task_id", taskId!),
                }),
            };
            task = await SendAsync(client, pollReq);
            status = task.GetProperty("status").GetString();
            Console.WriteLine($"status={status}");
        }

        if (status == "failed")
        {
            throw new Exception($"task failed: {task}");
        }
        if (!task.TryGetProperty("result_url", out var resultUrlProp) || resultUrlProp.GetString() is not string resultUrl || resultUrl.Length == 0)
        {
            throw new Exception($"task exported but no result_url: {task}");
        }

        var download = await client.GetAsync(resultUrl);
        if (!download.IsSuccessStatusCode)
        {
            throw new Exception($"download failed: {(int)download.StatusCode}");
        }
        await using var fs = File.Create("results.zip");
        await download.Content.CopyToAsync(fs);
        Console.WriteLine("saved results.zip");
    }
}
