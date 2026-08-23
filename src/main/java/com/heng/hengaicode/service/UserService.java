package com.heng.hengaicode.service;

import com.heng.hengaicode.model.dto.user.UserQueryRequest;
import com.heng.hengaicode.model.entity.User;
import com.heng.hengaicode.model.vo.LoginUserVO;
import com.heng.hengaicode.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author heng-ai-code
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param confirmPassword 确认密码
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String confirmPassword);

    /**
     * 用户登录
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request HttpServletRequest对象
     * @return 用户id
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户信息
     * @param request HttpServletRequest对象
     * @return 当前登录用户信息
     */
    User getCurrentLoginUser(HttpServletRequest request);

    /**
     * 用户退出
     * @param request HttpServletRequest对象
     * @return 是否退出成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的登录用户信息
     * @param user 用户实体
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏后的用户信息
     * @param user 用户实体
     * @return 脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表
     * @param users 用户实体列表
     * @return 脱敏后的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> users);

    /**
     * 将查询请求转为QueryWrapper
     * @param userQueryRequest 查询请求
     * @return QueryWrapper对象
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取加密后的密码
     * @param password 密码
     * @return 加密后的密码
     */
    String getEncryptedPassword(String password);
}
