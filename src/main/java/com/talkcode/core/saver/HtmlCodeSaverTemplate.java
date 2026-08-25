package com.talkcode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.talkcode.ai.model.HtmlCodeResult;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.model.enums.CodeGenTypeEnum;

public class HtmlCodeSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    /**
     * HTML
     */
    private static final String HTML_FILE_NAME = "index.html";

    @Override
    protected CodeGenTypeEnum getCodeGenType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath,HTML_FILE_NAME, result.getHtmlCode());
    }

    @Override
    protected void validate(HtmlCodeResult result) {
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
        }
    }
}
