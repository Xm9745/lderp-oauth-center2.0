package com.zhuoxuan.erp.config;


import com.zhuoxuan.erp.common.EmptyUtils;
import com.zhuoxuan.erp.pojo.user.LoginUser;
import com.zhuoxuan.erp.service.LoginUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 根据用户名获取用户<br>
 * <p>
 * 密码校验请看下面两个类
 *
 * @see org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
 * @see org.springframework.security.authentication.dao.DaoAuthenticationProvider
 */

@Service("userDetailsService")
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private LoginUserService loginUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        BCryptPasswordEncoder passwordEncode = new BCryptPasswordEncoder();

        LoginUser loginUser = new LoginUser();

        boolean isUserExist = false;
        try {

            loginUser = loginUserService.getLoginUserByUsername(username);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 通过用户名获取用户信息
        if (EmptyUtils.isNotEmpty(loginUser.getUsername())) {
            isUserExist = true;
        }

        if (isUserExist) {
            if (loginUser.getEnable()) {
                return loginUser;
            } else {
                throw new UsernameNotFoundException("用户[" + username + "]被冻结");
            }
        } else {
            throw new UsernameNotFoundException("用户[" + username + "]不存在");
        }

    }


}
