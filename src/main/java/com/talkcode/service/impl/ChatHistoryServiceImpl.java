package com.talkcode.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.talkcode.constant.UserConstant;
import com.talkcode.exception.ErrorCode;
import com.talkcode.exception.ThrowUtils;
import com.talkcode.mapper.ChatHistoryMapper;
import com.talkcode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.talkcode.model.entity.App;
import com.talkcode.model.entity.ChatHistory;
import com.talkcode.model.entity.User;
import com.talkcode.model.enums.ChatHistoryMessageTypeEnum;
import com.talkcode.service.AppService;
import com.talkcode.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author heng-ai-code
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 1. 校验参数
        ThrowUtils.throwIf(ObjUtil.isNull(appId) || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(ObjUtil.isNull(userId) || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 2. 校验消息类型是否存在
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(ObjUtil.isNull(messageTypeEnum), ErrorCode.PARAMS_ERROR, "不支持的消息类型");
        // 3.添加对话历史消息
        ChatHistory chatHistory = ChatHistory.builder()
                .message(message)
                .messageType(messageType)
                .appId(appId)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory ChatMemory, int maxMessages) {
        try {
            // 1.构造查询条件,起点为1而不是0,用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq("appId", appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxMessages);
            List<ChatHistory> chatHistoryList = this.list(queryWrapper);
            if (CollUtil.isEmpty(chatHistoryList)) {
                return 0;
            }
            // 2.反转列表保证时间为正序
            CollUtil.reverse(chatHistoryList);
            // 3.按时间顺序添加到记忆中,分类型添加（类型为用户或者AI）,加载前要清理历史缓存
            ChatMemory.clear();
            int loadedCount = 0;
            for (ChatHistory chatHistory : chatHistoryList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())) {
                    ChatMemory.add(UserMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())) {
                    ChatMemory.add(AiMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为应用{}加载对话历史消息到对话记忆中,共加载{}条消息", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("为应用{}加载对话历史消息到对话记忆中失败，异常信息：{}", appId, e.getMessage());
            // 加载失败不影响正常功能
            return 0;
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        Long id = chatHistoryQueryRequest.getId();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        queryWrapper
                .eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);

        // 游标查询逻辑
        if (ObjUtil.isNotNull(lastCreateTime)) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排序
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        // 1.参数校验
        ThrowUtils.throwIf(ObjUtil.isNull(appId) || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 页面大小必须在1到50条之间
        ThrowUtils.throwIf(pageSize < 1 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1到50条之间");
        ThrowUtils.throwIf(ObjUtil.isNull(loginUser) || loginUser.getId() <= 0, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 校验权限，只有用户本人和管理员才能查询
        App app = appService.getById(appId);
        ThrowUtils.throwIf(ObjUtil.isNull(app), ErrorCode.PARAMS_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = loginUser.getId().equals(app.getUserId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权限查看对话历史");
        // 3. 构建查询条件
        ChatHistoryQueryRequest chatHistoryQueryRequest = new ChatHistoryQueryRequest();
        chatHistoryQueryRequest.setAppId(appId);
        chatHistoryQueryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(chatHistoryQueryRequest);
        // 4. 执行查询
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(ObjUtil.isNull(appId) || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

}
