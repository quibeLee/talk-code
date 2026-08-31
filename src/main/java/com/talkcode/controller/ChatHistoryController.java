package com.talkcode.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.talkcode.annotation.AuthCheck;
import com.talkcode.common.BaseResponse;
import com.talkcode.common.ResultUtils;
import com.talkcode.constant.UserConstant;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.exception.ThrowUtils;
import com.talkcode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.talkcode.model.entity.App;
import com.talkcode.model.entity.ChatEventLog;
import com.talkcode.model.entity.ChatHistory;
import com.talkcode.model.entity.User;
import com.talkcode.service.AppService;
import com.talkcode.service.ChatEventLogService;
import com.talkcode.service.ChatHistoryService;
import com.talkcode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 控制层。
 *
 * @author heng-ai-code
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatEventLogService chatEventLogService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getCurrentLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 查询某一轮对话的结构化事件（用于前端展开查看推理/工具过程，整合自 Zero-code）
     */
    @GetMapping("/turn/{turnId}/events")
    public BaseResponse<List<ChatEventLog>> listTurnEvents(@PathVariable String turnId,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(turnId == null || turnId.isBlank(), ErrorCode.PARAMS_ERROR, "turnId 不能为空");
        User loginUser = userService.getCurrentLoginUser(request);
        List<ChatEventLog> eventLogs = chatEventLogService.listEventsByTurnId(turnId);
        if (eventLogs.isEmpty()) {
            return ResultUtils.success(List.of());
        }
        Long appId = eventLogs.get(0).getAppId();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        if (!isAdmin && !isCreator) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该轮对话事件");
        }
        return ResultUtils.success(eventLogs);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }


}
