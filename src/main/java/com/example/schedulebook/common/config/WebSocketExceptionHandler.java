package com.example.schedulebook.common.config;

import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.common.response.StompErrorResponse;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionHandler {
    @MessageExceptionHandler(BaseException.class)
    @SendToUser("/queue/errors")
    public StompErrorResponse handleBaseException(BaseException e) {
        return new StompErrorResponse(
                e.getErrorEnum().name(),
                e.getErrorEnum().getStatus(),
                e.getMessage()
        );
    }
}
