package com.talkcode.ai.service;
import com.talkcode.ai.model.HtmlCodeResult;
import com.talkcode.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

/**
 * AI代码生成服务（HTML / 多文件，不涉及工具调用）
 * 负责根据用户需求生成或修改 HTML、多文件代码
 * <p>
 * 创建场景与修改场景通过不同方法区分（不同提示词）：
 * - generateXxxCodeStream: 首次创建
 * - modifyXxxCodeStream: 修改迭代
 * <p>
 * Vue 项目（涉及工具调用）已拆分到 {@link AiCodeCreateService} 与 {@link AiCodeModifyService}
 */
public interface AiCodeGeneratorService {

    /**
     * 生成HTML代码（非流式，首次创建）
     * @param userMessage 用户消息
     * @return HTML代码字符串
     */
    @SystemMessage(fromResource = "prompt/codegen-html-create-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码（非流式，首次创建）
     * @param userMessage 用户消息
     * @return 多文件代码字符串
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-create-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成 HTML 代码（流式，首次创建）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-create-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码（流式，首次创建）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-create-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

    /**
     * 修改 HTML 代码（流式，基于已生成的网站做最小化调整）
     *
     * @param userMessage 用户消息
     * @return 修改后的完整代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-modify-system-prompt.txt")
    Flux<String> modifyHtmlCodeStream(String userMessage);

    /**
     * 修改多文件代码（流式，基于已生成的文件做最小化调整）
     *
     * @param userMessage 用户消息
     * @return 修改后的完整代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-modify-system-prompt.txt")
    Flux<String> modifyMultiFileCodeStream(String userMessage);

}
