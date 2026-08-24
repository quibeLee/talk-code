package com.heng.hengaicode.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.heng.hengaicode.constant.UserConstant;
import com.heng.hengaicode.exception.ErrorCode;
import com.heng.hengaicode.exception.ThrowUtils;
import com.heng.hengaicode.mapper.ChatHistoryMapper;
import com.heng.hengaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.heng.hengaicode.model.entity.App;
import com.heng.hengaicode.model.entity.ChatHistory;
import com.heng.hengaicode.model.entity.User;
import com.heng.hengaicode.model.enums.ChatHistoryMessageTypeEnum;
import com.heng.hengaicode.service.AppService;
import com.heng.hengaicode.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层实现。
 *
 * @author heng-ai-code
 */
@Service
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
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(ObjUtil.isNull(appId) || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
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
}
