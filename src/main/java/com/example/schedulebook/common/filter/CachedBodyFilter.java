package com.example.schedulebook.common.filter;

import com.example.schedulebook.common.security.CachedBodyHttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CachedBodyFilter extends OncePerRequestFilter {

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

        CachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest(request);

        filterChain.doFilter(cachedBodyHttpServletRequest, response);
    }
}
