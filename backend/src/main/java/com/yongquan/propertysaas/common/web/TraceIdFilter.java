package com.yongquan.propertysaas.common.web;

import com.yongquan.propertysaas.common.api.TraceId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = request.getHeader(TRACE_HEADER);
            if (traceId != null && !traceId.isBlank()) {
                TraceId.set(traceId);
            }
            response.setHeader(TRACE_HEADER, TraceId.current());
            filterChain.doFilter(request, response);
        } finally {
            TraceId.clear();
        }
    }
}
