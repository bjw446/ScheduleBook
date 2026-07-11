package com.example.schedulebook.common.filter;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisRateLimitService;
import com.example.schedulebook.common.response.ApiResponse;
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

        LoginRequest loginRequest;

        try {
            loginRequest = objectMapper.readValue(cachedBodyHttpServletRequest.getBody(), LoginRequest.class);

        } catch (IOException e) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);

        }

        String loginId = loginRequest.loginId();

        String ip = getClientIp(request);

        if (!validateRateLimit(RedisConst.LOGIN_IP_PREFIX + ip, response, ErrorEnum.LOGIN_IP_RATE_LIMITED)) {
            return;
        }

        if (!validateRateLimit(RedisConst.LOGIN_ID_PREFIX + loginId, response, ErrorEnum.LOGIN_ID_RATE_LIMITED)) {
            return;
        }

        filterChain.doFilter(cachedBodyHttpServletRequest, response);
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private boolean validateRateLimit(String key, HttpServletResponse response, ErrorEnum errorEnum) throws IOException {
        boolean allowed = redisRateLimitService.allowRequest(
                key,
                60_000,
                5,
                UUID.randomUUID().toString()
        );

        if (!allowed) {
            sendRateLimitResponse(response, errorEnum);
            return false;
        }

        return true;
    }

    private void sendRateLimitResponse(HttpServletResponse response, ErrorEnum errorEnum) throws IOException {
        response.setStatus(errorEnum.getStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.fail(errorEnum)
        );

        response.getWriter().flush();
    }
}
