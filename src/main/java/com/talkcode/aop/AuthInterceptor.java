package com.talkcode.aop;

import com.talkcode.annotation.AuthCheck;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.model.entity.User;
import com.talkcode.model.enums.UserEnum;
import com.talkcode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doIntercept(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = userService.getCurrentLoginUser(request);
        String mustRole = authCheck.mustRole();
        UserEnum mustRoleEnum = UserEnum.getUserEnum(mustRole);
        // 不需要权限，放行
        if (null == mustRoleEnum) {
            return joinPoint.proceed();
        }
        // 校验用户权限
        UserEnum userRoleEnum = UserEnum.getUserEnum(loginUser.getUserRole());
        // 用户无任何权限，抛出异常
        if (null == userRoleEnum) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求用户有管理员权限，但登录用户无管理员权限
        if (UserEnum.ADMIN.equals(mustRoleEnum) && !UserEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 普通用户，放行
        return joinPoint.proceed();
    }
}
