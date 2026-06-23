package com.example.schedulebook.common.config;

import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.StompErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Slf4j
public class WebSocketExceptionHandler {
    @MessageExceptionHandler(BaseException.class)
    @SendToUser("/queue/errors")
    public StompErrorResponse handleBaseException(BaseException e) {
        log.warn("WebSocket 메시지 처리 중 실패, code = {}, message = {}", e.getErrorEnum().name(), e.getMessage());

        return new StompErrorResponse(
                e.getErrorEnum().name(),
                e.getErrorEnum().getStatus(),
                e.getMessage()
        );
    }
}
