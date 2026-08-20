package com.heng.hengaicode.common;

import com.heng.hengaicode.exception.ErrorCode;

/**
 * 基础响应工具类
 */
public class ResultUtils {

    /**
     * 成功响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "success");
    }

    /**
     * 失败响应
     * @param errorCode 错误码
     * @return 响应包装类
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败响应
     * @param code 状态码
     * @param message 错误信息
     * @return 响应包装类
     */
    public static <T> BaseResponse<T> error(int code, String message ) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败响应
     * @param errorCode 状态码
     * @param message 数据
     * @return 响应包装类
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

}
