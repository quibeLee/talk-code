package com.talkcode.ai.model.message;

import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialToolCall;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具调用消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage {

    private String id;

    private String name;

    private String arguments;

    public ToolRequestMessage(PartialToolCall partialToolCall) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = partialToolCall.id();
        this.name = partialToolCall.name();
        this.arguments = partialToolCall.partialArguments();
    }

    public ToolRequestMessage(CompleteToolCall completeToolCall) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = completeToolCall.toolExecutionRequest().id();
        this.name = completeToolCall.toolExecutionRequest().name();
        this.arguments = completeToolCall.toolExecutionRequest().arguments();
    }
}