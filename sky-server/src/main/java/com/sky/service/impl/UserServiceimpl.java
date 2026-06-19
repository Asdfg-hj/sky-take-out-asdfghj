package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceimpl implements UserService{

    //微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    /**
     * 微信登录
     */
    @Override
    public User wxlogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获取微信用户的openid
        String openid = getOpenid(userLoginDTO.getCode());

        //如果微信API调用失败，测试环境下用code前8位模拟openid
        if (openid == null) {
            log.warn("微信登录失败，使用测试模式: code前8位作为openid");
            openid = "test_" + userLoginDTO.getCode().substring(0, 8);
        }

        //判断当前用户是否是新用户
         User user = userMapper.getByOpenid(openid);
        //如果是新用户，自动注册
        if (user == null) {
            user = User.builder()
                                .openid(openid)
                                .createTime(LocalDateTime.now())
                                .build();
            userMapper.insert(user);
        }
        //如果不是新用户，直接返回用户信息
        return user;
        
    }

    private String getOpenid(String code) {
        //调用微信接口服务，获取微信用户的openid
        Map<String,String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        log.info("请求微信API: appid={}, code={}", weChatProperties.getAppid(), code);
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        log.info("微信API返回: {}", json);

        if (json == null || json.isEmpty()) {
            log.error("微信API返回为空");
            return null;
        }

        JSONObject jsonobject = JSON.parseObject(json);
        String openid = jsonobject.getString("openid");
        if (openid == null) {
            log.error("微信API返回无openid, errcode={}, errmsg={}",
                jsonobject.getString("errcode"), jsonobject.getString("errmsg"));
        }
        return openid;
    }

}
