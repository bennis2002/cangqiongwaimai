package com.sky.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("adminSetmealController")
@Slf4j
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐管理")
public class SetmealController {

    @Resource
    SetmealService setmealService;

    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames = "SETMEAL" , key = "#setmealDTO.categoryId")
    public Result saveSetmeal(@RequestBody SetmealDTO setmealDTO) {
        setmealService.saveSetmeal(setmealDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询套餐")
    public Result<SetmealVO> queryById(@PathVariable Integer id) {
        SetmealVO setmealVO = setmealService.queryById(id);
        return Result.success(setmealVO);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询")

    public Result<PageResult> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("删除套餐")
    @CacheEvict(cacheNames = "SETMEAL" , allEntries = true)
    public Result DeleteByIds(Integer[] ids) {
        setmealService.deleteByIds(ids);
        return Result.success();
    }

    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "SETMEAL" , allEntries = true)
    public Result updateById(@RequestBody SetmealDTO setmealDTO) {
        setmealService.updateById(setmealDTO);
        return Result.success();
    }


    @PostMapping("/status/{status}")
    @ApiOperation("套餐状态设置")
    @CacheEvict(cacheNames = "SETMEAL" , allEntries = true)
    public Result updateStatus(@PathVariable Integer status , Integer id) {
        setmealService.updateStatus(status, id);
        return Result.success();
    }
}
