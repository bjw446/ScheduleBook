package com.example.schedulebook.common.exception;

import com.example.schedulebook.common.enums.ErrorEnum;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorEnum errorEnum;

    public BaseException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.errorEnum = errorEnum;
    }
}
