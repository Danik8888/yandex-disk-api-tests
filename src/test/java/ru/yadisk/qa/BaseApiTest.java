package ru.yadisk.qa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class BaseApiTest {
    protected static final String BASE_TEST_PATH = "/qa-tests";

    private static final Duration RESOURCE_WAIT_TIMEOUT = Duration.ofSeconds(30);

    protected static DiskApiClient client;
    protected static DiskApiClient unauthenticatedClient;

    private final List<String> createdPaths = new ArrayList<>();

    @BeforeAll
    static void initClients() {
        client = new DiskApiClient(resolveToken());
        unauthenticatedClient = new DiskApiClient("");
    }

    @AfterEach
    void cleanup() {
        for (String path : createdPaths) {
            silentlyDelete(path);
        }
    }
    protected String newFolderPath() {
        ensureBaseFolder();
        String path = BASE_TEST_PATH + "/" + UUID.randomUUID();
        track(path);
        return path;
    }

    protected String uniqueFilePath(String folderPath, String fileName) {
        String path = folderPath + "/" + fileName + "-" + shortUuid();
        track(path);
        return path;
    }

    protected void track(String path) {
        createdPaths.add(path);
    }
    protected void uploadSmallFile(String filePath, String content) {
        ApiResponse link = client.getUploadLink(filePath);
        assertEquals(200, link.statusCode(), "Не удалось получить URL для загрузки: " + link.body());

        ApiResponse upload = client.uploadFile(
                link.json().path("href").asText(),
                link.json().path("method").asText("PUT"),
                content.getBytes(StandardCharsets.UTF_8));
        assertTrue(upload.statusCode() == 201 || upload.statusCode() == 202,
                "Загрузка файла не удалась: " + upload.statusCode() + " " + upload.body());
        waitForResource(filePath, RESOURCE_WAIT_TIMEOUT);
    }
    protected void waitForResource(String path, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (client.getResource(path).statusCode() == 200) {
                return;
            }
            sleep(500);
        }
        fail("Ресурс не появился в течение " + timeout.toSeconds() + " с: " + path);
    }
    protected void waitForResourceAbsent(String path, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (client.getResource(path).statusCode() == 404) {
                return;
            }
            sleep(500);
        }
        fail("Ресурс не был удалён в течение " + timeout.toSeconds() + " с: " + path);
    }
    protected static ApiResponse assertStatus(ApiResponse response, int expectedCode) {
        assertEquals(expectedCode, response.statusCode(),
                "Неожиданный код ответа. Тело: " + response.body());
        return response;
    }
    protected static String resolveToken() {
        String token = System.getProperty("yadisk.token");
        if (token == null || token.isBlank()) {
            token = System.getenv("YANDEX_DISK_TOKEN");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Не задан OAuth-токен Яндекс.Диска.\n"
                            + "Задайте переменную окружения YANDEX_DISK_TOKEN либо передайте параметр\n"
                            + "-Dyadisk.token=<токен> при запуске mvn.\n"
                            + "Как получить токен — см. README в корне проекта.");
        }
        return token.trim();
    }
    private void ensureBaseFolder() {
        ApiResponse response = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                response = client.createFolder(BASE_TEST_PATH);
                if (response.statusCode() == 201 || response.statusCode() == 409) {
                    return;
                }
                if (response.statusCode() == 423) {
                    sleep(2_000);
                    continue;
                }
                break;
            } catch (DiskApiException e) {
                sleep(2_000);
            }
        }
        fail("Не удалось создать базовую папку " + BASE_TEST_PATH + ": "
                + (response == null ? "нет ответа" : response.statusCode() + " " + response.body()));
    }
    private void silentlyDelete(String path) {
        try {
            client.deleteResource(path, true);
        } catch (Exception ignored) {
        }
    }

    protected static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
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
