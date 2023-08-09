package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/shop")
@Api("商店管理")
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
        log.info("设置店铺的营业状态为 ： " , status == 1 ? "营业" : "打烊");
        shopService.setStatus(status);
        return Result.success();
    }
}
