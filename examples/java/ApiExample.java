import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiExample {

    private static final String BASE_URL = "https://api.checknumber.ai";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("CHECKNUMBER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set the CHECKNUMBER_API_KEY environment variable");
        }

        String body = submit(apiKey);
        String taskId = field(body, "task_id");
        String status = field(body, "status");
        System.out.println("submitted task " + taskId + " status=" + status);

        while (!"exported".equals(status) && !"failed".equals(status)) {
            Thread.sleep(5000);
            body = poll(apiKey, taskId);
            status = field(body, "status");
            System.out.println("status=" + status);
        }

        if ("failed".equals(status)) {
            throw new IllegalStateException("task failed: " + body);
        }
        String resultUrl = field(body, "result_url");
        if (resultUrl == null || resultUrl.isBlank()) {
            throw new IllegalStateException("task exported but no result_url: " + body);
        }

        download(resultUrl, "results.zip");
        System.out.println("saved results.zip");
    }

    private static String submit(String apiKey) throws IOException, InterruptedException {
        String boundary = "----ApiExampleBoundary";
        byte[] file = Files.readAllBytes(Path.of("../numbers.txt"));
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"task_type\"\r\n\r\nvk\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"numbers.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";
        byte[] headBytes = head.getBytes();
        byte[] tailBytes = tail.getBytes();
        byte[] payload = new byte[headBytes.length + file.length + tailBytes.length];
        System.arraycopy(headBytes, 0, payload, 0, headBytes.length);
        System.arraycopy(file, 0, payload, headBytes.length, file.length);
        System.arraycopy(tailBytes, 0, payload, headBytes.length + file.length, tailBytes.length);

        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/v1/tasks"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.ofByteArray(payload))
                .build();
        return send(req);
    }

    private static String poll(String apiKey, String taskId) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/v1/gettasks"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString("task_id=" + taskId))
                .build();
        return send(req);
    }

    private static String send(HttpRequest req) throws IOException, InterruptedException {
        HttpResponse<String> resp = CLIENT.send(req, BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IllegalStateException("request failed: " + resp.statusCode() + " " + resp.body());
        }
        return resp.body();
    }

    private static void download(String url, String dest) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 300) {
            throw new IllegalStateException("download failed: " + resp.statusCode());
        }
        Files.write(Path.of(dest), resp.body());
    }

    // Minimal flat-JSON field reader; the task API always returns a flat
    // object, so a full JSON library is not needed for this example.
    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
