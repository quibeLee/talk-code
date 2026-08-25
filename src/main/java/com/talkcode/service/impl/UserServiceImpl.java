package com.talkcode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.mapper.UserMapper;
import com.talkcode.model.dto.user.UserQueryRequest;
import com.talkcode.model.entity.User;
import com.talkcode.model.enums.UserEnum;
import com.talkcode.model.vo.LoginUserVO;
import com.talkcode.model.vo.UserVO;
import com.talkcode.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.talkcode.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author heng-ai-code
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String confirmPassword) {
        //校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号、密码、确认密码不能为空");
        }
        // 密码账号不能小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码账号不能小于4位");
        }
        // 密码长度不能小于6位
        if (userPassword.length() < 6 || confirmPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于6位");
        }
        if (!userPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和确认密码不能一致");
        }
        //检查用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        //密码加密
        String encryptedPassword = getEncryptedPassword(userPassword);
        //创建用户
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        user.setUserName("用户" + userAccount);
        user.setUserRole(UserEnum.USER.getValue());
        boolean isSuccess = this.save(user);
        if (!isSuccess) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败,请联系管理员");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @return 用户id
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号、密码不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        //2.加密
        String encryptedPassword = getEncryptedPassword(userPassword);
        //3.检查用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptedPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (null == user) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或账号密码错误");
        }
        //4.记录用户状态信息
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        //5.返回脱敏后数据
        return getLoginUserVO(user);
    }

    @Override
    public User getCurrentLoginUser(HttpServletRequest request) {
        // 1.判断当前登录用户是否在会话中
        User currentLoginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (null == currentLoginUser) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2.根据当前用户id获取最新用户信息
        currentLoginUser = this.getById(currentLoginUser.getId());
        if (null == currentLoginUser) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 3.返回脱敏后数据
        return currentLoginUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1.判断当前登录用户是否在会话中
        User currentLoginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (null == currentLoginUser) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 1.清除用户状态信息
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不能为空");
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public String getEncryptedPassword(String password) {
        //盐值加密
        final String salt = "heng";
        return DigestUtils.md5DigestAsHex((salt + password).getBytes());
    }


}
