package ru.yadisk.qa;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PutDiskApiTest extends BaseApiTest {

    @Test
    void createFolderReturns201AndFolderAppears() {
        String folderPath = newFolderPath();

        ApiResponse create = client.createFolder(folderPath);

        assertStatus(create, 201);
        waitForResource(folderPath, Duration.ofSeconds(30));

        ApiResponse meta = client.getResource(folderPath);
        assertStatus(meta, 200);
        assertEquals("dir", meta.json().path("type").asText(), "Созданный ресурс должен быть папкой");
    }

    @Test
    void duplicateFolderReturns409() {
        String folderPath = newFolderPath();

        assertStatus(client.createFolder(folderPath), 201);
        ApiResponse secondAttempt = client.createFolder(folderPath);

        assertStatus(secondAttempt, 409);
    }

    @Test
    void createFolderWithMissingParentReturnsError() {
        String orphanPath = BASE_TEST_PATH + "/ghost-" + shortUuid() + "/child";
        track(orphanPath);

        ApiResponse response = client.createFolder(orphanPath);

        assertTrue(response.statusCode() == 404 || response.statusCode() == 409,
                "Ожидался 404 или 409, получено: " + response.statusCode() + " " + response.body());
    }

    @Test
    void createFolderWithoutTokenReturns401() {
        DiskApiClient unauthenticatedClient = new DiskApiClient("");

        ApiResponse response = unauthenticatedClient.createFolder(BASE_TEST_PATH + "/no-auth-" + shortUuid());

        assertStatus(response, 401);
    }

    @Test
    void uploadFileViaPutUploadsContentToDisk() {
        String folderPath = newFolderPath();
        assertStatus(client.createFolder(folderPath), 201);
        String filePath = uniqueFilePath(folderPath, "hello.txt");

        ApiResponse link = client.getUploadLink(filePath);
        assertStatus(link, 200);
        assertEquals("PUT", link.json().path("method").asText("PUT"));

        ApiResponse upload = client.uploadFile(
                link.json().path("href").asText(),
                link.json().path("method").asText("PUT"),
                "Hello, Yandex Disk! Привет, Яндекс.Диск!".getBytes(StandardCharsets.UTF_8));
        assertTrue(upload.statusCode() == 201 || upload.statusCode() == 202,
                "PUT на href вернул неожиданный код: " + upload.statusCode() + " " + upload.body());

        waitForResource(filePath, Duration.ofSeconds(30));
        ApiResponse meta = client.getResource(filePath);
        assertStatus(meta, 200);
        assertEquals("file", meta.json().path("type").asText(), "Загруженный ресурс должен быть файлом");
        assertTrue(meta.json().path("size").asLong() > 0, "Размер файла должен быть больше 0");
    }
}