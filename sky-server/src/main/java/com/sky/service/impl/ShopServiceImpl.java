package com.sky.service.impl;

import com.sky.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
        stringRedisTemplate.opsForValue().set("SHOP_STATUS", String.valueOf(status));
    }


    @Override
    public Integer getStatus() {
        String s = stringRedisTemplate.opsForValue().get("SHOP_STATUS");
        return Integer.valueOf(s);
    }
}
