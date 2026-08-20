package com.heng.hengaicode.controller;

import com.heng.hengaicode.common.BaseResponse;
import com.heng.hengaicode.common.ResultUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检查健康测试接口
 */
@RestController
@RequestMapping("/health")
public class HealthChekController {

    @RequestMapping("/")
    public BaseResponse<String> check() {
        return ResultUtils.success("ok");
    }
}

