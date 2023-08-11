package com.sky.controller.user;


import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/user/shop")
@Api(tags = "商店接口")
public class ShopController {
    @Resource
    private ShopService shopService;

    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus(){

        Integer i = shopService.getStatus();
        log.info("获取店铺状态: {}", i == 1 ? "营业" : "打烊");
        return Result.success(i);
    }
}
