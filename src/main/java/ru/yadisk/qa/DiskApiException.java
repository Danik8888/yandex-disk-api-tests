package ru.yadisk.qa;

public class DiskApiException extends RuntimeException {

    public DiskApiException(String message) {
        super(message);
    }

    public DiskApiException(String message, Throwable cause) {
        super(message, cause);
    }
}