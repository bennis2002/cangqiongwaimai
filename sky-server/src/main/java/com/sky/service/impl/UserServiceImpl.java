package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Resource
    UserMapper userMapper;

    //微信登录接口地址：
    private final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Resource
    private WeChatProperties weChatProperties;

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {

        String openid = getOpenId(userLoginDTO.getCode());

        //判断是否正确获取id,openid为空则抛出业务异常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //判断当前用户是否为新用户
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, User::getOpenid, openid);

        User user = userMapper.selectOne(lqw);
        //如果是新用户则自动完成注册
        if (user == null) {
            user = new User();
            user.setCreateTime(LocalDateTime.now());
            user.setOpenid(openid);
            userMapper.insert(user);
        }

        //返回用户对象
        return user;
    }

    private String getOpenId(String code) {
        //调用微信服务接口服务 ， 获得openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");

        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);
        return  jsonObject.getString("openid");
    }
}
