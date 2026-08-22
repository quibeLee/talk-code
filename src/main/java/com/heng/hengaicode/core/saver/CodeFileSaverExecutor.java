package com.heng.hengaicode.core.saver;

import com.heng.hengaicode.ai.model.HtmlCodeResult;
import com.heng.hengaicode.ai.model.MultiFileCodeResult;
import com.heng.hengaicode.exception.BusinessException;
import com.heng.hengaicode.exception.ErrorCode;
import com.heng.hengaicode.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存器执行器
 */
public class CodeFileSaverExecutor {

    /**
     * HTML代码保存器模板
     */
    private static final HtmlCodeSaverTemplate htmlCodeSaverTemplate = new HtmlCodeSaverTemplate();

    /**
     * 多文件代码保存器模板
     */
    private static final MultiFileCodeSaverTemplate multiFileCodeSaverTemplate = new MultiFileCodeSaverTemplate();


    /**
     * 执行代码文件保存
     * @param result 代码结果
     * @param codeGenTypeEnum 代码类型枚举
     */
    public static File execute(Object result, CodeGenTypeEnum codeGenTypeEnum) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeSaverTemplate.saveCode((HtmlCodeResult) result);
            case MULTI_FILE -> multiFileCodeSaverTemplate.saveCode((MultiFileCodeResult) result);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码类型 : " + codeGenTypeEnum.getValue());
        };
    }
}
