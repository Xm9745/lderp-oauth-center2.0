package com.zhuoxuan.erp.config;


import com.alibaba.fastjson.JSON;
import com.zhuoxuan.erp.pojo.user.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import java.util.HashMap;
import java.util.Map;

/**
 * 授权服务器配置
 *
 * @author xiaoming
 */
@Configuration
@EnableAuthorizationServer   //授权服务器的注解
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    /**
     * 认证管理器
     *
     * @see SecurityConfig 的authenticationManagerBean()
     */
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    /**
     * 使用jwt或者redis<br>
     * 默认redis
     */
//    @Value("${access_token.store-jwt:false}")   //配置文件中自定义的一个参数，表示access_token是否采用jwt
    private boolean storeWithJwt = true;
    /**
     * 登陆后返回的json数据是否追加当前用户信息<br>
     * 默认false
     */
//    @Value("${access_token.add-userinfo:false}")
    private boolean addUserInfo = true;
    @Autowired
    private RedisAuthorizationCodeServices redisAuthorizationCodeServices;
    @Autowired
    private RedisClientDetailsService redisClientDetailsService;

    /**
     * 令牌存储
     */
    @Bean
    public TokenStore tokenStore() {
        if (storeWithJwt) {         //如果storeWithJwt为true，则使用JwtTokenStore进行存储，否则使用RedisTokenStore进行存储
            return new JwtTokenStore(accessTokenConverter());
        }
        RedisTokenStore redisTokenStore = new RedisTokenStore(redisConnectionFactory);//使用RedisTokenStore进行存储的是userId
        // 解决同一username每次登陆access_token都相同的问题
        redisTokenStore.setAuthenticationKeyGenerator(new RandomAuthenticationKeyGenerator());
        return redisTokenStore;
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(this.authenticationManager);
        endpoints.tokenStore(tokenStore());
        // 授权码模式下，code存储
//		endpoints.authorizationCodeServices(new JdbcAuthorizationCodeServices(dataSource));
        endpoints.authorizationCodeServices(redisAuthorizationCodeServices);
        if (storeWithJwt) {
            endpoints.accessTokenConverter(accessTokenConverter());
        } else {
            // 将当前用户信息追加到登陆后返回数据里
            endpoints.tokenEnhancer((accessToken, authentication) -> {
                addLoginUserInfo(accessToken, authentication);
                return accessToken;
            });
        }
    }

    /**
     * 将当前用户信息追加到登陆后返回的json数据里<br>
     * 通过参数access_token.add-userinfo控制<br>
     *
     * @param accessToken
     * @param authentication
     */
    private void addLoginUserInfo(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        if (!addUserInfo) {
            return;
        }
        if (accessToken instanceof DefaultOAuth2AccessToken) {
            DefaultOAuth2AccessToken defaultOAuth2AccessToken = (DefaultOAuth2AccessToken) accessToken;
            Authentication userAuthentication = authentication.getUserAuthentication();
            Object principal = userAuthentication.getPrincipal();
            System.out.println("用户名1  : " + JSON.toJSONString(principal));
            if (principal instanceof LoginUser) {
                System.out.println("用户名2  : " + JSON.toJSONString(principal));
                LoginUser loginUser = (LoginUser) principal;            //********************************

                Map<String, Object> map = new HashMap<>(defaultOAuth2AccessToken.getAdditionalInformation()); // 旧的附加参数
                map.put("loginUser", loginUser); // 追加当前登陆用户
                System.out.println("用户名3  : " + JSON.toJSONString(loginUser));
                defaultOAuth2AccessToken.setAdditionalInformation(map);
            }
        }
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        // 开启/oauth/token_key验证端口无权限访问
        security.tokenKeyAccess("permitAll()")
                // 开启/oauth/check_token验证端口认证权限访问
                .checkTokenAccess("isAuthenticated()")
                .passwordEncoder(passwordEncoder)
                .allowFormAuthenticationForClients(); // 允许表单形式的认证
    }


    /**
     * 我们将client信息存储到oauth_client_details表里<br>
     * 并将数据缓存到redis
     *
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

//        clients.inMemory()
//                .withClient("system")
//                .authorizedGrantTypes("password", "authorization_code", "refresh_token")
//                .scopes("app")
//                .authorities("ADMIN")
//                .secret("system")
//                .accessTokenValiditySeconds(3600)
//                .refreshTokenValiditySeconds(3600);


//		clients.jdbc(dataSource);
        // 2018.06.06，这里优化一下，详细看下redisClientDetailsService这个实现类
        clients.withClientDetails(redisClientDetailsService);
        redisClientDetailsService.loadAllClientToCache();
    }

    @Autowired
    public UserDetailsService userDetailsService;
    /**
     * jwt签名key，可随意指定<br>
     * 如配置文件里不设置的话，冒号后面的是默认值
     */
    @Value("${access_token.jwt-signing-key:xiaoweijiagou}")
    private String signingKey;

    /**
     * Jwt资源令牌转换器<br>
     * 参数access_token.store-jwt为true时用到
     *
     * @return accessTokenConverter
     */
//    @Bean
//    public JwtAccessTokenConverter accessTokenConverter() {
//
//        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter() {
//            @Override
//            public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
//
////                addLoginUserInfo(oAuth2AccessToken, authentication); // 将当前用户信息追加到登陆后返回数据里
//
//
//                Authentication userAuthentication = authentication.getUserAuthentication();
//                Object principal = userAuthentication.getPrincipal();
//
//                final Map<String, Object> map = new HashMap<>();
//                DefaultOAuth2AccessToken defaultOAuth2AccessToken = (DefaultOAuth2AccessToken) accessToken;
//
//                if (principal instanceof LoginUser) {
//                    LoginUser loginUser = (LoginUser) principal;
//                    map.put("loginUser", loginUser); // 追加当前登陆用户
//                    defaultOAuth2AccessToken.setAdditionalInformation(map);
//                }
//                OAuth2AccessToken oAuth2AccessToken = super.enhance(accessToken, authentication);
//                return oAuth2AccessToken;
//            }
//        };

//        // 将当前用户信息详情追加到access_token 认证后返回数据的 principal 里
//        DefaultAccessTokenConverter defaultAccessTokenConverter = (DefaultAccessTokenConverter) jwtAccessTokenConverter
//                .getAccessTokenConverter();
//        DefaultUserAuthenticationConverter userAuthenticationConverter = new DefaultUserAuthenticationConverter();
//        userAuthenticationConverter.setUserDetailsService(userDetailsService);
//        defaultAccessTokenConverter.setUserTokenConverter(userAuthenticationConverter);
//
//        //  这里务必设置一个，否则多台认证中心的话，一旦使用jwt方式，access_token将解析错误
//        jwtAccessTokenConverter.setSigningKey("SigningKey");
//
//        return jwtAccessTokenConverter;
//    }



    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter() {
            @Override
            public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
                OAuth2AccessToken oAuth2AccessToken = super.enhance(accessToken, authentication);
                addLoginUserInfo(oAuth2AccessToken, authentication); // 将当前用户信息追加到登陆后返回数据里
                return oAuth2AccessToken;
            }
        };

        // 将当前用户信息详情追加到access_token 认证后返回数据的 principal 里
        DefaultAccessTokenConverter defaultAccessTokenConverter = (DefaultAccessTokenConverter) jwtAccessTokenConverter
                .getAccessTokenConverter();
        DefaultUserAuthenticationConverter userAuthenticationConverter = new DefaultUserAuthenticationConverter();
        userAuthenticationConverter.setUserDetailsService(userDetailsService);
        defaultAccessTokenConverter.setUserTokenConverter(userAuthenticationConverter);

        //  这里务必设置一个，否则多台认证中心的话，一旦使用jwt方式，access_token将解析错误
        jwtAccessTokenConverter.setSigningKey("SigningKey");

        return jwtAccessTokenConverter;
    }

}
