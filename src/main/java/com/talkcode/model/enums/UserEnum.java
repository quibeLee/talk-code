package com.talkcode.model.enums;

import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 用户类型美剧
 */
@Getter
public enum UserEnum {


    USER("普通用户","user"),
    ADMIN("管理员","admin");

    private final String text;

    private final String value;

    UserEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     * @param value 枚举值
     * @return 枚举对象
     */
    public static UserEnum getUserEnum(String value) {
        // 非空校验
        if (StringUtils.isBlank(value)) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        for (UserEnum userEnum : UserEnum.values()) {
            if (userEnum.value.equals(value)) {
                return userEnum;
            }
        }
        return null;
    }
}
