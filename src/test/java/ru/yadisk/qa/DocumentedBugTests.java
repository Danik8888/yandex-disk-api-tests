package ru.yadisk.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DocumentedBugTests extends BaseApiTest {

    private static final String PUBLIC_FILE_URL =
            "https://raw.githubusercontent.com/yandex-disk/yandex-disk-restapi-java/master/LICENSE";
    @Disabled("Расхождение: PUT /v1/disk/resources с отсутствующим родителем возвращает 409, а не 404.")
    @Test
    void createFolderWithMissingParentShouldReturn404AccordingToDocs() {
        String orphanPath = BASE_TEST_PATH + "/ghost-" + shortUuid() + "/child";
        track(orphanPath);

        assertStatus(client.createFolder(orphanPath), 404);
    }
    @Disabled("Расхождение: POST /v1/disk/resources/upload в отсутствующую папку возвращает 409, а не 404.")
    @Test
    void saveFileFromUrlToMissingFolderShouldReturn404AccordingToDocs() {
        String ghostPath = BASE_TEST_PATH + "/ghost-" + shortUuid() + "/file.txt";
        track(ghostPath);

        assertStatus(client.saveFileFromUrl(PUBLIC_FILE_URL, ghostPath), 404);
    }
}
