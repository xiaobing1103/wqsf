package com.wqst.api.common;

import java.time.OffsetDateTime;
import org.slf4j.MDC;

public record ApiResponse<T>(boolean success, String code, String message, T data,
                             String requestId, OffsetDateTime timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "操作成功", data, MDC.get("requestId"), OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "CREATED", "创建成功", data, MDC.get("requestId"), OffsetDateTime.now());
    }

    public static ApiResponse<Object> error(String code, String message, Object data) {
        return new ApiResponse<>(false, code, message, data, MDC.get("requestId"), OffsetDateTime.now());
    }
}
