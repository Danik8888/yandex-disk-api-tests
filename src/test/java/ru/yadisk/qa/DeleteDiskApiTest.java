package ru.yadisk.qa;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteDiskApiTest extends BaseApiTest {

    @Test
    void deleteEmptyFolderReturns204() {
        String folderPath = newFolderPath();
        assertStatus(client.createFolder(folderPath), 201);
        ApiResponse delete = client.deleteResource(folderPath, true);

        assertEquals(204, delete.statusCode(), "Тело ответа: " + delete.body());
        waitForResourceAbsent(folderPath, Duration.ofSeconds(30));
        assertStatus(client.getResource(folderPath), 404);
    }
    @Test
    void deleteUploadedFileReturns204() {
        String folderPath = newFolderPath();
        assertStatus(client.createFolder(folderPath), 201);
        String filePath = uniqueFilePath(folderPath, "to-delete.txt");
        uploadSmallFile(filePath, "Context: текст для удаления");

        ApiResponse delete = client.deleteResource(filePath, true);
        assertEquals(204, delete.statusCode(), "Тело ответа: " + delete.body());
        waitForResourceAbsent(filePath, Duration.ofSeconds(30));
        assertStatus(client.getResource(filePath), 404);
    }

    @Test
    void deleteNonEmptyFolderReturns202AndEventuallyDisappears() {
        String folderPath = newFolderPath();
        assertStatus(client.createFolder(folderPath), 201);
        uploadSmallFile(uniqueFilePath(folderPath, "inside.txt"), "файл внутри папки");
        ApiResponse delete = client.deleteResource(folderPath, true);
        assertTrue(delete.statusCode() == 202 || delete.statusCode() == 204,
                "Ожидался 202 или 204, получено: " + delete.statusCode() + " " + delete.body());
        if (delete.statusCode() == 202) {
            String operationHref = delete.json().path("href").asText("");
            assertFalse(operationHref.isBlank(), "В ответе 202 должна быть ссылка на операцию");
            client.waitForOperation(operationHref, Duration.ofSeconds(60));
        }
        waitForResourceAbsent(folderPath, Duration.ofSeconds(30));
        assertStatus(client.getResource(folderPath), 404);
    }

    @Test
    void deleteNonExistentResourceReturns404() {
        String ghostPath = BASE_TEST_PATH + "/ghost-" + shortUuid();

        ApiResponse delete = client.deleteResource(ghostPath, true);

        assertStatus(delete, 404);
    }
}
