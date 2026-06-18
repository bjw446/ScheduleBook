package com.example.schedulebook.common.exception;

import com.example.schedulebook.common.enums.ErrorEnum;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorEnum ErrorEnum;

    public BaseException(ErrorEnum ErrorEnum) {
        super(ErrorEnum.getMessage());
        this.ErrorEnum = ErrorEnum;
    }
}
