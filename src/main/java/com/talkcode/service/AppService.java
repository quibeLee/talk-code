package com.talkcode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.talkcode.model.dto.app.AppAddRequest;
import com.talkcode.model.dto.app.AppQueryRequest;
import com.talkcode.model.entity.App;
import com.talkcode.model.entity.User;
import com.talkcode.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author heng-ai-code
 */
public interface AppService extends IService<App> {

    /**
     * chat 应用服务
     *
     * @param appId     应用ID
     * @param message   用户消息
     * @param loginUser 登录用户
     * @return 代码流
     */
    default Flux<String> chatToGenCode(long appId, String message, User loginUser) {
        return chatToGenCode(appId, message, loginUser, null);
    }

    /**
     * chat 应用服务（支持选择生成模式：classic 工具调用 / workflow 工作流）
     *
     * @param appId     应用ID
     * @param message   用户消息
     * @param loginUser 登录用户
     * @param mode      生成模式（classic/workflow），为空时默认 classic
     * @return 代码流
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser, String mode);

    /**
     * 封装脱敏后的应用信息
     *
     * @param app 应用实体
     * @return 脱敏后的应用信息
     */
    AppVO getAppVO(App app);

    /**
     * 查询应用信息
     *
     * @param appQueryRequest 查询参数
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 封装应用信息列表
     *
     * @param appList 应用实体列表
     * @return 应用信息列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 分页查询应用信息包装方法
     *
     * @param queryWrapper 查询包装器
     * @param pageNum      页码
     * @param pageSize     每页数量
     * @return 应用列表
     */
    Page<AppVO> getAppVOPage(QueryWrapper queryWrapper, long pageNum, long pageSize);

    /**
     * 部署应用
     *
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署标识
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 创建应用
     *
     * @param appAddRequest 应用创建请求
     * @param loginUser 登录用户
     * @return 应用ID
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);
}
