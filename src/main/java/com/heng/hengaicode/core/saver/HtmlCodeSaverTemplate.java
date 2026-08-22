package com.heng.hengaicode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.heng.hengaicode.ai.model.HtmlCodeResult;
import com.heng.hengaicode.exception.BusinessException;
import com.heng.hengaicode.exception.ErrorCode;
import com.heng.hengaicode.model.enums.CodeGenTypeEnum;

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
