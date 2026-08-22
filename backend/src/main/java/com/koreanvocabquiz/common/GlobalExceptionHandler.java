package com.koreanvocabquiz.common;

import java.util.List;

import com.koreanvocabquiz.admin.AdminAuthenticationException;
import com.koreanvocabquiz.quiz.QuizGenerationException;
import com.koreanvocabquiz.quiz.QuizSubmissionException;
import com.koreanvocabquiz.statistics.QuizHistoryCompletionException;
import com.koreanvocabquiz.vocabulary.InvalidCsvException;
import com.koreanvocabquiz.vocabulary.VocabularyImageExtractionException;
import com.koreanvocabquiz.literature.LiteratureValidationException;
import com.koreanvocabquiz.literature.LiteraryImageAnalysisException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(AdminAuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAdminAuthentication(AdminAuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
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

    @ExceptionHandler(LiteratureValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleLiteratureValidation(LiteratureValidationException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), List.of(exception.getMessage())));
    }

    @ExceptionHandler(LiteraryImageAnalysisException.class)
    public ResponseEntity<ApiErrorResponse> handleLiteraryImageAnalysis(LiteraryImageAnalysisException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), List.of(exception.getMessage())));
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiErrorResponse.of(
                        HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                        List.of("Uploaded images are too large. Use images up to 10MB each and 50MB per request.")
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

    @ExceptionHandler(QuizHistoryCompletionException.class)
    public ResponseEntity<ApiErrorResponse> handleQuizHistoryCompletion(QuizHistoryCompletionException exception) {
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
