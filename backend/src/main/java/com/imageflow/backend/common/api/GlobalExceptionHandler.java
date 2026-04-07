package com.imageflow.backend.common.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.common.exception.NotFoundException;
import com.imageflow.backend.common.exception.UnauthorizedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("NOT_FOUND", exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("UNAUTHORIZED", exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("BAD_REQUEST", exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MultipartException.class
    })
    public ResponseEntity<ApiErrorResponse> handleRequestBinding(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("BAD_REQUEST", exception.getMessage(), Instant.now()));
    }
}
