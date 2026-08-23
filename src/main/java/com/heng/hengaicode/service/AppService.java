package com.heng.hengaicode.service;

import com.heng.hengaicode.model.dto.app.AppQueryRequest;
import com.heng.hengaicode.model.entity.App;
import com.heng.hengaicode.model.vo.AppVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author heng-ai-code
 */
public interface AppService extends IService<App> {
    /**
     * 封装脱敏后的应用信息
     * @param app 应用实体
     * @return 脱敏后的应用信息
     */
    public AppVO getAppVO(App app);

    /**
     * 查询应用信息
     * @param appQueryRequest 查询参数
     * @return 查询条件
     */
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 封装应用信息列表
     * @param appList 应用实体列表
     * @return 应用信息列表
     */
    public List<AppVO> getAppVOList(List<App> appList);

    /**
     * 分页查询应用信息包装方法
     * @param queryWrapper 查询包装器
     * @param pageNum      页码
     * @param pageSize     每页数量
     * @return 应用列表
     */
    public Page<AppVO> getAppVOPage(QueryWrapper queryWrapper, long pageNum, long pageSize);
}
