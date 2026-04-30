package com.switchwon.forex.common.response;

import com.switchwon.forex.common.exception.ErrorCode;

public record ApiResponse<T>(
        String code,
        String message,
        T returnObject
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "정상적으로 처리되었습니다.", data);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
