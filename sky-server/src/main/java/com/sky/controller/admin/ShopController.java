package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "商店管理")
@Slf4j
public class ShopController {
    @Resource
    ShopService shopService;

    /**
     * 设置店铺的营业状态
     */
    @PutMapping("/{status}")
    @ApiOperation("商店状态设置")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺的营业状态为 ： {}", status == 1 ? "营业" : "打烊");
        shopService.setStatus(status);
        return Result.success();
    }

    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus(){

        Integer i = shopService.getStatus();
        log.info("获取店铺状态: {}", i == 1 ? "营业" : "打烊");
        return Result.success(i);
    }
}
