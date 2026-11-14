package com.elephantjava.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elephantjava.contex.VisitorContext;
import com.elephantjava.entity.VisitorSession;
import com.elephantjava.mapper.VisitorSessionMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Component
public class VisitorSessionInterceptor implements HandlerInterceptor {

    private final VisitorSessionMapper sessionMapper;

    public VisitorSessionInterceptor(VisitorSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod()) && uri.endsWith("/v1/visitor-sessions")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && uri.contains("/v1/public/")) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        String token = header.substring(7).trim();
        VisitorSession session = sessionMapper.selectOne(new LambdaQueryWrapper<VisitorSession>()
                .eq(VisitorSession::getTokenHash, sha256(token)));
        if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            response.setStatus(401);
            return false;
        }
        VisitorContext.set(session.getId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        VisitorContext.clear();
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
