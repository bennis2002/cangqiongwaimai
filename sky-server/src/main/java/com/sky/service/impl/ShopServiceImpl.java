package com.sky.service.impl;

import com.sky.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class ShopServiceImpl implements ShopService {
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public void setStatus(Integer status) {

    }
}
