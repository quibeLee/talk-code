package com.talkcode.core.parser;

import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.model.enums.CodeGenTypeEnum;

/**
 * 代码解析器执行器
 */
public class CodeParserExecutor {


    private static  final HtmlCodeParser  htmlCodeParser = new HtmlCodeParser();


    private static  final MultiFileCodeParser  multiFileCodeParser = new MultiFileCodeParser();
    /**
     * 执行代码解析
     *
     * @param codeContent 代码内容
     * @param codeGenType  代码解析器
     * @return 解析结果
     */
    public static Object execute(String codeContent, CodeGenTypeEnum codeGenType) throws Exception {
        return switch (codeGenType) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> {
                String errorMessage = String.format("不支持的生成类型:%s", codeGenType.getValue());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
}
