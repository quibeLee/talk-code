package com.talkcode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.talkcode.ai.model.MultiFileCodeResult;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.model.enums.CodeGenTypeEnum;

public class MultiFileCodeSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    /**
     * HTML
     */
    private static final String HTML_FILE_NAME = "index.html";
    /**
     * CSS
     */
    private static final String CSS_FILE_NAME = "style.css";
    /**
     * JS
     */
    private static final String JS_FILE_NAME = "script.js";

    @Override
    protected CodeGenTypeEnum getCodeGenType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, HTML_FILE_NAME, result.getHtmlCode());
        writeToFile(baseDirPath, CSS_FILE_NAME, result.getCssCode());
        writeToFile(baseDirPath, JS_FILE_NAME, result.getJsCode());
    }

    @Override
    protected void validate(MultiFileCodeResult result) {
        // 至少由HTML代码片段
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码片段不能为空");
        }
    }
}
