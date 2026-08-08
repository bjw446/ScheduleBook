package com.example.schedulebook.common.filter;

import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.ApiResponse;
import com.example.schedulebook.common.security.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CachedBodyFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!request.getServletPath().equals("/auth/login")) {
            filterChain.doFilter(request, response);

            return;
        }

        if (request instanceof CachedBodyHttpServletRequest) {
            filterChain.doFilter(request, response);

            return;
        }

        try {
            CachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest(request);

            filterChain.doFilter(cachedBodyHttpServletRequest, response);

        } catch (BaseException e) {
            response.setStatus(e.getErrorEnum().getStatus());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.fail(e.getErrorEnum())
            );

            response.flushBuffer();

            return;
        }
    }
}