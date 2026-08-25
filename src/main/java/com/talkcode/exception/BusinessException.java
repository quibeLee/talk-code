package com.talkcode.exception;

import lombok.Getter;

/**
 * 业务异常
 * @author heng
 * @date   2026/8/20
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code.getCode();
    }

}