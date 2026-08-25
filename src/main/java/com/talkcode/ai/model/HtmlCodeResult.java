package com.talkcode.ai.model;

import jdk.jfr.Description;
import lombok.Data;

@Data
@Description("HTML代码结果")
public class HtmlCodeResult {

    /**
     * HTML代码
     */
    @Description("HTML代码")
       private String htmlCode;
    /**
     * 生成代码的描述
     */
    @Description("生成代码的描述")
    private String description;
}
