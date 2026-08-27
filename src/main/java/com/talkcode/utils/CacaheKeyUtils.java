package com.talkcode.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/**
 * 缓存键工具类
 */
public class CacaheKeyUtils {

    /**
     * 生成缓存键, 对象转换为 JSON 字符串, 并使用 MD5 加密
     *
     * @param obj 对象
     * @return 缓存键
     */
    public static String generateCacheKey(Object obj) {
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}
