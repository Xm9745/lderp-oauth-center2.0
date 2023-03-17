package com.zhuoxuan.erp.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 资源服务配置<br>
 * <p>
 * 注解@EnableResourceServer帮我们加入了org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter<br>
 * 该filter帮我们从request里解析出access_token<br>
 * 并通过org.springframework.security.oauth2.provider.token.DefaultTokenServices根据access_token和认证服务器配置里的TokenStore从redis或者jwt里解析出用户
 * <p>
 * 注意认证中心的@EnableResourceServer和别的微服务里的@EnableResourceServer有些不同<br>
 * 别的微服务是通过org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices来获取用户的
 *
 * @author xiaoming
 */
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {


//        http.csrf().disable()
//                .exceptionHandling()
//                .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
//                .and()
//                .authorizeRequests()
////                .antMatchers(PermitAllUrl.permitAllUrl("/users-anon/**", "/wechat/**")).permitAll() // 放开权限的url
//                .antMatchers("/oauth/**","api/**").permitAll() // 放开权限的url
//                .anyRequest().authenticated()
//                .and()
//                .httpBasic();

        http.authorizeRequests()
                .antMatchers("/oauth/**").permitAll() // 放开权限的url，认证中心和别的服务不同之处
                .anyRequest().authenticated().and()
                .httpBasic().and().csrf().disable();


    }

}
