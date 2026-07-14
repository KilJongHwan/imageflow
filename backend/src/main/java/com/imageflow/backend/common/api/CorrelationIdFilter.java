package com.imageflow.backend.common.api;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청이 처리되는 동안 correlation id를 로깅 MDC에 넣어, 그 요청에서 찍힌 모든 로그 라인을
 * 하나로 묶어 추적할 수 있게 한다. id는 응답 헤더로 되돌려 주고, 들어온 {@code X-Correlation-Id}가
 * 있으면 그대로 사용해 (호출자나 향후 게이트웨이가) 서비스 간 추적을 이어붙일 수 있다.
 *
 * <p>application.yaml의 로그 패턴이 {@code %X{corrId}}를 렌더링하는데, 이 필터가 없으면 그 자리가
 * 그냥 비어 있다. 즉 요청 단위 correlation을 실제로 찍히게 하는 게 이 필터다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "corrId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 스레드는 풀에서 재사용되므로, 남은 id가 다음 요청으로 새어 나가면 안 된다.
            MDC.remove(MDC_KEY);
        }
    }
}
