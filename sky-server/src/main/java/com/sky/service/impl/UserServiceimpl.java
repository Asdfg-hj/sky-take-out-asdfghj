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

@Service
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

        
        //判断openid是否为空，如果为空，说明微信登录失败，抛出业务异常
        if (openid == null) {
            throw new RuntimeException(MessageConstant.LOGIN_FAILED);
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
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonobject = JSON.parseObject(json);
        String openid = jsonobject.getString("openid");
        return openid;
    }

}
