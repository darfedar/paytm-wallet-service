package com.paytm.wallet.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) rq;
        HttpServletResponse s = (HttpServletResponse) rs;
        String id = r.getHeader("X-Correlation-Id");
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        MDC.put("correlationId", id);
        s.setHeader("X-Correlation-Id", id);
        try {
            chain.doFilter(rq, rs);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
