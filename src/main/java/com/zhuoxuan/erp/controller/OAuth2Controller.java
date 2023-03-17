package com.zhuoxuan.erp.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping
public class OAuth2Controller {


    @Autowired
    private ConsumerTokenServices tokenServices;

    /**
     * 注销登陆/退出
     * 移除access_token和refresh_token<br>
     * 2018.06.28 改为用ConsumerTokenServices，该接口的实现类DefaultTokenServices已有相关实现，我们不再重复造轮子
     *
     * @param access_token
     */
    @DeleteMapping(value = "/remove_token", params = "access_token")
    public boolean removeToken(String access_token) {
        System.out.println(access_token);
        boolean flag = tokenServices.revokeToken(access_token);
        if (flag) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println(authentication.getName());
        }
        return flag;
    }


}
