package com.talkcode.controller;

import com.talkcode.common.BaseResponse;
import com.talkcode.common.ResultUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检查健康测试接口
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @RequestMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}

