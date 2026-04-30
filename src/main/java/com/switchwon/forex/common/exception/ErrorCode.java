package com.switchwon.forex.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    EXCHANGE_RATE_NOT_FOUND("EXCHANGE_RATE_NOT_FOUND", "환율 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CURRENCY("INVALID_CURRENCY", "지원하지 않는 통화입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_REQUEST("INVALID_ORDER_REQUEST", "주문 요청이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
