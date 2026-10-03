package ru.yadisk.qa;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public class DiskApiClient {

    public static final String BASE_URL = "https://cloud-api.yandex.net";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final String token;

    public DiskApiClient(String token) {
        this.token = token == null ? "" : token.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    public ApiResponse getDiskInfo() {
        return execute("GET", BASE_URL + "/v1/disk", null, null);
    }

    public ApiResponse getResource(String path) {
        return execute("GET", BASE_URL + "/v1/disk/resources", Map.of("path", path), null);
    }

    public ApiResponse getUploadLink(String path) {
        return execute("GET", BASE_URL + "/v1/disk/resources/upload", Map.of("path", path), null);
    }
    public ApiResponse createFolder(String path) {
        return execute("PUT", BASE_URL + "/v1/disk/resources", Map.of("path", path), null);
    }

    public ApiResponse deleteResource(String path, boolean permanently) {
        return execute("DELETE", BASE_URL + "/v1/disk/resources",
                Map.of("path", path, "permanently", Boolean.toString(permanently)), null);
    }
    public ApiResponse saveFileFromUrl(String url, String path) {
        return execute("POST", BASE_URL + "/v1/disk/resources/upload",
                Map.of("url", url, "path", path), null);
    }

    public ApiResponse uploadFile(String href, String httpMethod, byte[] content) {
        return execute(httpMethod, href, null, content, false);
    }

    public ApiResponse getOperation(String href) {
        return execute("GET", href, null, null);
    }
    public void waitForOperation(String href, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            ApiResponse response = getOperation(href);
            JsonNode json = response.json();
            String status = json == null ? "" : json.path("status").asText("");
            if ("success".equals(status)) {
                return;
            }
            if ("failed".equals(status)) {
                throw new IllegalStateException(
                        "Операция завершилась с ошибкой: " + response.body() + ", href=" + href);
            }
            sleep(1_000);
        }
        throw new IllegalStateException("Операция не завершилась за " + timeout.toSeconds() + " с: " + href);
    }
    private ApiResponse execute(String method, String url, Map<String, String> queryParams, byte[] body) {
        return execute(method, url, queryParams, body, true);
    }

    private ApiResponse execute(String method, String url, Map<String, String> queryParams, byte[] body,
                                boolean withAuth) {
        String target = withQuery(url, queryParams);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(target))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");

        if (withAuth && !token.isEmpty()) {
            builder.header("Authorization", "OAuth " + token);
        }
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body);
        HttpRequest request = builder.method(method, publisher).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new DiskApiException("Ошибка при выполнении " + method + " " + target, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DiskApiException("Запрос был прерван: " + method + " " + target, e);
        }
    }
    private static String withQuery(String url, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }
        String query = queryParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return url + "?" + query;
    }
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание было прервано", e);
        }
    }
}
