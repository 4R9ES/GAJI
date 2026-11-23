package com.GAJI.GAJI.global.exception;

import com.GAJI.GAJI.global.response.ErrorCode;

/**
 * 서비스에서 "이건 실패다"라고 알릴 때 던진다.
 * 예) throw new BusinessException(ErrorCode.USER_LOGIN_ID_DUPLICATE);
 * GlobalExceptionHandler가 잡아서 명세서 형식의 실패 응답으로 바꿔 준다.
 * RuntimeException이라 @Transactional 안에서 던지면 자동으로 롤백된다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
