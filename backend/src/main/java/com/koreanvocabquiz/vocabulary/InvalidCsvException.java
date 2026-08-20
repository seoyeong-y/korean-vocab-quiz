package com.koreanvocabquiz.vocabulary;

public class InvalidCsvException extends RuntimeException {

    public InvalidCsvException(String message) {
        super(message);
    }
}
