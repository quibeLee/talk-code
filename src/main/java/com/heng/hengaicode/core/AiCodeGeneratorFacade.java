package com.heng.hengaicode.core;

import com.heng.hengaicode.ai.AiCodeGeneratorService;
import com.heng.hengaicode.ai.model.HtmlCodeResult;
import com.heng.hengaicode.ai.model.MultiFileCodeResult;
import com.heng.hengaicode.core.parser.CodeParserExecutor;
import com.heng.hengaicode.core.saver.CodeFileSaverExecutor;
import com.heng.hengaicode.exception.BusinessException;
import com.heng.hengaicode.exception.ErrorCode;
import com.heng.hengaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * 代码生成器Facade,组合AI代码生成和保存服务
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口,根据生成类型,保存文件后返回代码文件 (非流式生成)
     *
     * @param userMessage     用户消息
     * @param codeGenTypeEnum 生成类型
     * @return 生成的代码文件
     */
    public File generateCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.execute(htmlCodeResult, codeGenTypeEnum);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.execute(multiFileCodeResult, codeGenTypeEnum);
            }
            default -> {
                String errorMessage = String.format("不支持的生成类型:%s", codeGenTypeEnum.getValue());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, codeGenTypeEnum);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, codeGenTypeEnum);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 生成HTML代码（流式）
     *
     * @param codeStream      代码流
     * @param codeGenTypeEnum 生成类型
     * @return HTML代码流式结果
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum) {
        // 当流式生成完成时，保存代码到文件
        StringBuilder codeBuilder = new StringBuilder();
        //实时收集代码片段
        return codeStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            try {
                String completeCode = codeBuilder.toString();
                // 使用代码解析执行器解析代码
                Object result = CodeParserExecutor.execute(completeCode, codeGenTypeEnum);
                // 使用代码保存执行器保存代码
                CodeFileSaverExecutor.execute(result, codeGenTypeEnum);
            } catch (Exception e) {
                log.error("保存HTML代码到文件失败：{}", e.getMessage());
            }

        });
    }

}
