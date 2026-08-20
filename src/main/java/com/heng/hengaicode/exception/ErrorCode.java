package com.heng.hengaicode.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(20000, "操作成功"),
    FAIL(50000, "系统异常"),

    PARAM_ERROR(40000, "参数错误"),
    PARAM_MISSING(40001, "缺少必填参数"),
    PARAM_TYPE_ERROR(40002, "参数类型错误"),
    PARAM_LENGTH_ERROR(40003, "参数长度不合法"),

    BUSINESS_ERROR(40004, "业务异常"),
    DATA_NOT_FOUND(40005, "数据不存在"),
    DATA_ALREADY_EXISTS(40006, "数据已存在"),
    DATA_FORMAT_ERROR(40007, "数据格式错误"),

    UNAUTHORIZED(40008, "未授权"),
    FORBIDDEN(40009, "无权限"),
    TOKEN_EXPIRED(40010, "Token已过期"),
    TOKEN_INVALID(40011, "Token无效"),

    RESOURCE_NOT_FOUND(40012, "资源未找到"),
    METHOD_NOT_ALLOWED(40013, "请求方法不允许"),

    SYSTEM_ERROR(50000, "系统内部错误"),
    DATABASE_ERROR(50001, "数据库异常"),
    NETWORK_ERROR(50002, "网络异常"),
    THIRD_PARTY_ERROR(50003, "第三方服务异常");

    private final int code;
    private final String message;
}
