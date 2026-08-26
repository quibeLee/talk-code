package com.talkcode.core;

import cn.hutool.json.JSONUtil;
import com.talkcode.ai.AiCodeGeneratorServiceFactory;
import com.talkcode.ai.model.HtmlCodeResult;
import com.talkcode.ai.model.MultiFileCodeResult;
import com.talkcode.ai.model.message.AiResponseMessage;
import com.talkcode.ai.model.message.ToolExecutedMessage;
import com.talkcode.ai.model.message.ToolRequestMessage;
import com.talkcode.ai.service.AiCodeGeneratorService;
import com.talkcode.constant.AppConstant;
import com.talkcode.core.builder.VueProjectBuilder;
import com.talkcode.core.parser.CodeParserExecutor;
import com.talkcode.core.saver.CodeFileSaverExecutor;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
     * <p>
     * 自动区分【创建】与【修改】场景：
     * - 应用输出目录已存在且包含文件 → 修改（使用修改提示词/服务/最小工具集）
     * - 否则 → 创建（使用创建提示词/服务，Vue 只暴露文件写入工具）
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
        boolean modify = isModifyRequest(appId, codeGenTypeEnum);
        log.info("应用 {} 生成类型 {} 本次为{}请求", appId, codeGenTypeEnum.getValue(), modify ? "修改" : "创建");
        return switch (codeGenTypeEnum) {
            case HTML, MULTI_FILE -> {
                AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
                Flux<String> codeStream = switch (codeGenTypeEnum) {
                    case HTML -> modify
                            ? aiCodeGeneratorService.modifyHtmlCodeStream(userMessage)
                            : aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                    case MULTI_FILE -> modify
                            ? aiCodeGeneratorService.modifyMultiFileCodeStream(userMessage)
                            : aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                    default -> throw new IllegalStateException("unreachable");
                };
                yield processCodeStream(codeStream, codeGenTypeEnum, appId);
            }
            case VUE_PROJECT -> {
                // 创建场景：只暴露文件写入工具；修改场景：暴露 读/改/写/删/列目录 工具
                TokenStream tokenStream = modify
                        ? aiCodeGeneratorServiceFactory.getAiCodeModifyService(appId).modifyVueProjectStream(appId, userMessage)
                        : aiCodeGeneratorServiceFactory.getAiCodeCreateService(appId).generateVueProjectStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 判断本次请求是【创建】还是【修改】：
     * 应用对应的代码输出目录已存在且包含文件 → 修改；否则 → 创建
     */
    private boolean isModifyRequest(long appId, CodeGenTypeEnum codeGenTypeEnum) {
        String projectDirName = codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT
                ? "vue_project_" + appId
                : codeGenTypeEnum.getValue() + "_" + appId;
        File projectDir = new File(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
        File[] files = projectDir.listFiles();
        return projectDir.exists() && projectDir.isDirectory() && files != null && files.length > 0;
    }

    /**
     * 1.将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     * 2.生成代码（Vue项目）,并保存到文件
     * <p>
     * 注意：
     * 工具调用请求处理,工具调用参数流式到达时触发，前端可实时展示"正在调用工具..."
     * <p>
     * 兜底说明（非标准提供商 name 为空的问题）：
     * {@link PartialToolCall} 构造函数中 {@code this.name = ensureNotBlank(builder.name, "name")}，
     * 当 name 为空会抛出 IllegalArgumentException。
     * OpenAI 流式协议里工具调用 delta 的 name 分布在第一个 chunk（chunk1: name="writeFile"，后续 chunk 为 null），
     * langchain4j 内部 ToolCallBuilder 会累积第一个 chunk 的 name，所以标准 OpenAI 提供商 name 始终有值。
     * 但非标准提供商(如 Qwen、Deepseek)可能全程不携带 name，导致 PartialToolCall 构造失败。
     * <p>
     * 由于 langchain4j 1.19.0 的 {@link TokenStream} 并未提供 {@code onCompleteToolCall} 回调，
     * 这里改用现有 API 实现等价兜底：
     * <ul>
     *   <li>{@code onPartialToolCall}：实时上报部分工具调用，并记录已上报的工具调用 id（用于去重）。</li>
     *   <li>{@code onToolExecuted}：工具真正执行时携带完整请求信息(id/name/arguments/result)，
     *       若该工具调用此前因 name 为空未通过 onPartialToolCall 上报，则先补发一条完整的工具请求消息。</li>
     *   <li>{@code onCompleteResponse}：最终响应中仍携带且未上报的工具调用，再补发完整请求消息作为兜底。</li>
     * </ul>
     *
     * @param tokenStream TokenStream 对象
     * @param appId       应用ID
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, long appId) {
        return Flux.create(sink -> {
            // 记录已上报给前端的工具调用 id，用于去重（部分提供商可能不返回 id，此时不判重、直接兜底上报）
            Set<String> reportedToolRequestIds = ConcurrentHashMap.newKeySet();
            tokenStream
                    .onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })

                    // 工具调用请求处理,工具调用参数流式到达时触发，前端可实时展示"正在调用工具..."
                    .onPartialToolCall((PartialToolCall partialToolCall) -> {
                        try {
                            if (partialToolCall.id() != null) {
                                reportedToolRequestIds.add(partialToolCall.id());
                            }
                            ToolRequestMessage toolRequestMessage = new ToolRequestMessage(partialToolCall);
                            sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                        } catch (Exception e) {
                            // 防御：个别提供商构造 partialToolCall 时异常，避免中断整个流，交由后续完整工具调用信息兜底
                            log.warn("处理部分工具调用失败，等待完整工具调用信息兜底: {}", e.getMessage());
                        }
                    })

                    // 工具执行完成：携带完整工具调用信息(id/name/arguments/result)。
                    // 非标准提供商(name 为空)导致 PartialToolCall 未上报时，这里作为等价兜底，先补发完整请求再上报结果。
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutionRequest request = toolExecution.request();
                        if (request.id() == null || reportedToolRequestIds.add(request.id())) {
                            ToolRequestMessage completeRequestMessage = new ToolRequestMessage(new CompleteToolCall(0, request));
                            sink.next(JSONUtil.toJsonStr(completeRequestMessage));
                        }
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })

                    .onCompleteResponse((ChatResponse response) -> {
                        // 兜底：最终响应中仍携带、且此前未上报的工具调用，再补发完整请求信息
                        List<ToolExecutionRequest> toolExecutionRequests = response.aiMessage().toolExecutionRequests();
                        if (toolExecutionRequests != null) {
                            for (int i = 0; i < toolExecutionRequests.size(); i++) {
                                ToolExecutionRequest request = toolExecutionRequests.get(i);
                                if (request.id() == null || reportedToolRequestIds.add(request.id())) {
                                    ToolRequestMessage completeRequestMessage = new ToolRequestMessage(new CompleteToolCall(i, request));
                                    sink.next(JSONUtil.toJsonStr(completeRequestMessage));
                                }
                            }
                        }
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
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
                File saveDir = CodeFileSaverExecutor.execute(result, codeGenTypeEnum, appId);
                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存代码到文件失败：{}", e.getMessage());
            }
        });
    }

}
