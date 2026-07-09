package com.example.schedulebook.common.filter;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisRateLimitService;
import com.example.schedulebook.common.security.CachedBodyHttpServletRequest;
import com.example.schedulebook.domain.auth.dto.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final RedisRateLimitService redisRateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().equals("/auth/login");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!(request instanceof CachedBodyHttpServletRequest cachedBodyHttpServletRequest)) {
            filterChain.doFilter(request, response);

            return;
        }

        LoginRequest loginRequest = objectMapper.readValue(cachedBodyHttpServletRequest.getBody(), LoginRequest.class);

        String loginId = loginRequest.loginId();

        String ip = getClientIp(request);

        validateRateLimit(RedisConst.LOGIN_IP_PREFIX + ip, ErrorEnum.LOGIN_IP_RATE_LIMITED);

        validateRateLimit(RedisConst.LOGIN_ID_PREFIX + loginId, ErrorEnum.LOGIN_ID_RATE_LIMITED);

        filterChain.doFilter(cachedBodyHttpServletRequest, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0];
        }

        return request.getRemoteAddr();
    }

    private void validateRateLimit(String key, ErrorEnum errorEnum) {
        boolean allowed = redisRateLimitService.allowRequest(
                key,
                60_000,
                5,
                UUID.randomUUID().toString()
        );

        if (!allowed) {
            throw new BaseException(errorEnum);
        }
    }
}
