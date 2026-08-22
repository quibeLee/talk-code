package com.heng.hengaicode.ai.model;

import jdk.jfr.Description;
import lombok.Data;

@Data
@Description("生成多个文件代码结果")
public class MultiFileCodeResult {

    /**
     * HTML代码
     */
    @Description("HTML代码")
    private String htmlCode;
    /**
     * js代码
     */
    @Description("js代码")
    private String jsCode;
    /**
     * css代码
     */
    @Description("css代码")
    private String cssCode;
    /**
     * 生成代码的描述
     */
    @Description("生成代码的描述")
    private String description;
}
