package com.heng.hengaicode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.heng.hengaicode.ai.model.MultiFileCodeResult;
import com.heng.hengaicode.exception.BusinessException;
import com.heng.hengaicode.exception.ErrorCode;
import com.heng.hengaicode.model.enums.CodeGenTypeEnum;

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
