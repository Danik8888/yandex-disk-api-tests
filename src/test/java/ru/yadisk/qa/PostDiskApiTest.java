package ru.yadisk.qa;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostDiskApiTest extends BaseApiTest {

    private static final String PUBLIC_FILE_URL =
            "https://raw.githubusercontent.com/yandex-disk/yandex-disk-restapi-java/master/LICENSE";
    @Test
    void saveFileFromUrlReturns202AndFileAppearsAfterOperation() {
        String folderPath = newFolderPath();
        assertStatus(client.createFolder(folderPath), 201);
        String filePath = uniqueFilePath(folderPath, "license.txt");

        ApiResponse response = client.saveFileFromUrl(PUBLIC_FILE_URL, filePath);
        assertEquals(202, response.statusCode(), "Тело ответа: " + response.body());
        String operationHref = response.json().path("href").asText("");
        assertFalse(operationHref.isBlank(), "В ответе 202 должна быть ссылка на операцию (href)");

        client.waitForOperation(operationHref, Duration.ofSeconds(60));
        waitForResource(filePath, Duration.ofSeconds(30));
        ApiResponse meta = client.getResource(filePath);
        assertStatus(meta, 200);
        assertEquals("file", meta.json().path("type").asText(), "Сохранённый ресурс должен быть файлом");
        assertTrue(meta.json().path("size").asLong() > 0, "Размер файла должен быть больше 0");
    }

    @Test
    void saveFileFromUrlWithoutTokenReturns401() {
        ApiResponse response = unauthenticatedClient.saveFileFromUrl(
                PUBLIC_FILE_URL, BASE_TEST_PATH + "/no-auth.txt");

        assertStatus(response, 401);
    }

    @Test
    void saveFileFromUrlToMissingFolderReturnsError() {
        String ghostPath = BASE_TEST_PATH + "/ghost-" + shortUuid() + "/file.txt";
        track(ghostPath);

        ApiResponse response = client.saveFileFromUrl(PUBLIC_FILE_URL, ghostPath);
        assertTrue(response.statusCode() == 400 || response.statusCode() == 404 || response.statusCode() == 409,
                "Ожидался код 4xx, получено: " + response.statusCode() + " " + response.body());
    }
}
