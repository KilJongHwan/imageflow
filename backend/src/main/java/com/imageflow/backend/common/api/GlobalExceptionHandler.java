package com.imageflow.backend.common.api;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    /**
     * 위의 개별 핸들러가 잡지 못한 예외를 받는 최후의 방어선. 이게 없을 때는 예상 못 한 예외가
     * Spring 기본 /error로 빠지면서 애플리케이션 로그를 하나도 안 남겼고, 그래서 운영 500을
     * 응답 본문 모양으로 추론할 수밖에 없었다. 이제는 스택 트레이스를 (요청의 correlation id와 함께)
     * 서버에 남기면서, 클라이언트에는 안정적이고 내부 정보를 흘리지 않는 에러 본문을 돌려준다.
     *
     * <p>주의: 이건 {@link Throwable}이 아니라 {@link Exception}만 잡는다. OutOfMemoryError 같은
     * {@link Error}는 일부러 건드리지 않는다 — 프로세스 안에서 신뢰성 있게 복구할 수 없으므로,
     * 대신 예방(업로드 용량/힙 제한)하고 컨테이너 레벨에서 감시한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR", "예기치 못한 오류가 발생했습니다.", Instant.now()));
    }
}
