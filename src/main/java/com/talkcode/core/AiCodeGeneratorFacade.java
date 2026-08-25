package com.talkcode.core;

import cn.hutool.json.JSONUtil;
import com.talkcode.ai.AiCodeGeneratorService;
import com.talkcode.ai.AiCodeGeneratorServiceFactory;
import com.talkcode.ai.model.HtmlCodeResult;
import com.talkcode.ai.model.MultiFileCodeResult;
import com.talkcode.ai.model.message.AiResponseMessage;
import com.talkcode.ai.model.message.ToolExecutedMessage;
import com.talkcode.ai.model.message.ToolRequestMessage;
import com.talkcode.constant.AppConstant;
import com.talkcode.core.builder.VueProjectBuilder;
import com.talkcode.core.parser.CodeParserExecutor;
import com.talkcode.core.saver.CodeFileSaverExecutor;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
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
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 统一入口,根据生成类型,保存文件后返回代码文件 (非流式生成)
     *
     * @param userMessage     用户消息
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用ID
     * @return 生成的代码文件
     */
    public File generateCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型不能为空");
        }
        // 根据appId获取对应的代码生成器服务
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.execute(htmlCodeResult, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.execute(multiFileCodeResult, codeGenTypeEnum, appId);
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
     * @param appId           应用ID
     * @return 生成的代码流
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据appId获取对应的代码生成器服务
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, codeGenTypeEnum, appId);
            }
            case VUE_PROJECT -> {
                // 对于 Vue 项目代码，对返回的TokenStream进行处理,并传递工具调用信息
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 1.将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     * 2.生成代码（Vue项目）,并保存到文件
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    //工具调用请求处理,工具调用参数流式到达时触发，前端可实时展示"正在调用工具..."
                    .onPartialToolCall((PartialToolCall partialToolCall) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(partialToolCall);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }


    /**
     * 生成代码（HTML,多文件）,并保存到文件
     *
     * @param codeStream      代码流
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用ID
     * @return HTML代码流式结果
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum, long appId) {
        // 当流式生成完成时，保存代码到文件
        StringBuilder codeBuilder = new StringBuilder();
        //实时收集代码片段
        return codeStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            try {
                String completeCode = codeBuilder.toString();
                // 使用代码解析执行器解析代码
                Object result = CodeParserExecutor.execute(completeCode, codeGenTypeEnum);
                // 使用代码保存执行器保存代码
                CodeFileSaverExecutor.execute(result, codeGenTypeEnum, appId);
            } catch (Exception e) {
                log.error("保存代码到文件失败：{}", e.getMessage());
            }
        });
    }

}
