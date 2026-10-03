# yandex-disk-rest-api-tests

Автотесты на публичный REST API Яндекс.Диска (https://cloud-api.yandex.net).

Стек: Java 17+, JUnit 5, Maven. HTTP-запросы через java.net.http.HttpClient

## Запуск

Из корня репозитория:

```
mvn test
```

Токен передаётся через переменную окружения `YANDEX_DISK_TOKEN` или параметром `-Dyadisk.token=<токен>`. 

Как получить токен:
1. Завести отдельный тестовый аккаунт Яндекс: https://passport.yandex.ru/registration/ 
2. Войдити в него и получить OAuth-токен в полигоне: https://yandex.ru/dev/disk/poligon/
3. Задать токен через переменную окружения и запустите `mvn test`.

Ожидаемый результат прогона:

```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 2
BUILD SUCCESS
```

## Что покрыто

Тесты закрывают четыре HTTP-метода:

- **GET** `/v1/disk` и `/v1/disk/resources`. Данные о диске, метаданные файлов и папок, проверки на 401 и 404.
- **PUT** `/v1/disk/resources`. Создание папок (в том числе повторное создание с кодом 409), загрузка файла на href из upload-ссылки.
- **POST** `/v1/disk/resources/upload`. Сохранение файла из интернета по прямой ссылке, асинхронная операция (202).
- **DELETE** `/v1/disk/resources`. Удаление файлов и папок, в том числе асинхронное удаление непустой папки (202).

Нашел расхождение документации (yandex.ru/dev/disk/rest/) с реальным API:

1. `PUT /v1/disk/resources`: создаю папку, но если родительской папки нет, API отдаёт `409 DiskPathDoesntExistsError`. По документации это кейс с кодом 404.
2. `POST /v1/disk/resources/upload`: сохранение файла в несуществующую папку даёт то же самое, 409 вместо 400/404.
