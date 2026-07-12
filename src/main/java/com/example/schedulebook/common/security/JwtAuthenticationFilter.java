package com.example.schedulebook.common.security;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.redis.RedisBlacklistService;
import com.example.schedulebook.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final RedisBlacklistService redisBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            try {
                if (redisBlacklistService.isBlacklisted(token)) {
                    throw new BaseException(ErrorEnum.LOGOUT_TOKEN);
                }

                jwtProvider.validateToken(token);

                Long userId = jwtProvider.extractUserId(token);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of()
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (BaseException e) {
                log.warn("JWT 인증 실패 : {}", e.getMessage());

                sendError(response, e.getErrorEnum().getStatus(), e.getErrorEnum());

                return;

            } catch (Exception e) {
                log.warn("JWT 인증 중 예외 발생 : {}", e.getMessage());

                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorEnum.TOKEN_INVALID);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private void sendError(HttpServletResponse response, int status, ErrorEnum errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }
}
