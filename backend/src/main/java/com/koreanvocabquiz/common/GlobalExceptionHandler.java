package com.koreanvocabquiz.common;

import java.util.List;

import com.koreanvocabquiz.quiz.QuizGenerationException;
import com.koreanvocabquiz.quiz.QuizSubmissionException;
import com.koreanvocabquiz.vocabulary.InvalidCsvException;
import com.koreanvocabquiz.vocabulary.VocabularyImageExtractionException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        List.of(exception.getMessage())
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        messages
                ));
    }

    @ExceptionHandler(InvalidCsvException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCsv(InvalidCsvException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        List.of(exception.getMessage())
                ));
    }

    @ExceptionHandler(VocabularyImageExtractionException.class)
    public ResponseEntity<ApiErrorResponse> handleVocabularyImageExtraction(VocabularyImageExtractionException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        List.of(exception.getMessage())
                ));
    }

    @ExceptionHandler(QuizGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handleQuizGeneration(QuizGenerationException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        List.of(exception.getMessage())
                ));
    }

    @ExceptionHandler(QuizSubmissionException.class)
    public ResponseEntity<ApiErrorResponse> handleQuizSubmission(QuizSubmissionException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        List.of(exception.getMessage())
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        List.of("Request body contains invalid or unreadable values.")
                ));
    }
}
