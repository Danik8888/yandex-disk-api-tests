package ru.yadisk.qa;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetDiskApiTest extends BaseApiTest {

    @Test
    void diskInfoReturns200AndContainsExpectedFields() {
        ApiResponse response = client.getDiskInfo();

        assertStatus(response, 200);
        JsonNode json = response.json();
        assertTrue(json.has("total_space"), "В ответе GET /v1/disk нет поля total_space");
        assertTrue(json.has("used_space"), "В ответе GET /v1/disk нет поля used_space");
        assertTrue(json.has("system_folders"), "В ответе GET /v1/disk нет поля system_folders");
        assertTrue(json.path("total_space").asLong() > 0, "total_space должно быть больше 0");
    }

    @Test
    void rootFolderMetadataHasTypeDirAndEmbeddedItems() {
        ApiResponse response = client.getResource("/");

        assertStatus(response, 200);
        assertEquals("dir", response.json().path("type").asText());
        assertTrue(response.json().path("_embedded").path("items").isArray(),
                "В ответе должен быть список вложенных элементов _embedded.items");
    }

    @Test
    void nonExistentResourceReturns404() {
        String ghostPath = BASE_TEST_PATH + "/missing-" + UUID.randomUUID();

        ApiResponse response = client.getResource(ghostPath);

        assertStatus(response, 404);
    }

    @Test
    void requestWithoutTokenReturns401() {
        DiskApiClient unauthenticatedClient = new DiskApiClient("");

        ApiResponse response = unauthenticatedClient.getDiskInfo();

        assertStatus(response, 401);
    }
}