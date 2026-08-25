package com.talkcode.service;

import com.talkcode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.talkcode.model.entity.ChatHistory;
import com.talkcode.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author heng-ai-code
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话历史消息
     *
     * @param appId       应用id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      创建用户id
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 删除应用下的所有对话历史消息
     *
     * @param appId 应用id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);


    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest 查询包装类
     * @return 分页查询结果
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 游标查询对话历史消息
     *
     * @param appId          应用id
     * @param pageSize       每页数量
     * @param lastCreateTime 最后创建时间
     * @param loginUser      登录用户
     * @return 分页查询结果
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    /**
     * 将对话历史消息加载给缓存服务
     *
     * @param appId 应用id
     * @return 加载成功的消息数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory messageWindowChatMemory, int maxMessages);
}
