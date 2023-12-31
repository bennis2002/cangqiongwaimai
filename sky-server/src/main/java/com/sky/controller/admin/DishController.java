package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品管理")
public class DishController {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private DishService dishService;

    @PostMapping
    @ApiOperation("新增菜品")
    @CacheEvict(cacheNames = "DISH" , key = "#dish.categoryId")
    public Result save(@RequestBody DishDTO dish) {
        log.info("新增菜品");
        return dishService.saveWithFlavor(dish);
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    @Cacheable(cacheNames = "DISH")
    public Result<PageResult> selectByPage(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询");
        PageResult pageResult = dishService.PageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("删除菜品")
    @CacheEvict(cacheNames = "DISH" , allEntries = true)
    public Result deleteByIds(Integer[] ids) {
        log.info("菜品删除");
        dishService.deleteByIds(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> queryById(@PathVariable Integer id) {
        log.info("根据id查询菜品");
        DishVO dish = dishService.queryById(id);
        return Result.success(dish);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    @CacheEvict(cacheNames = "DISH" , allEntries = true)
    public Result updateById(@RequestBody DishDTO dishVO) {
        log.info("修改菜品");
        dishService.updateDish(dishVO);

        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类查询")
    @Cacheable(cacheNames = "DISH" , key = "#categoryId")
    public Result<List<Dish>> queryByCategoryId(Integer categoryId) {
        log.info("查询分类ID为{}的菜品信息" , categoryId);
        List<Dish> list = dishService.queryByCategoryId(categoryId);
        return Result.success(list);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售，停售")
    @CacheEvict(cacheNames = "DISH" , allEntries = true)
    public Result setStatus(@PathVariable Integer status, Integer id) {
        log.info("菜品编号:{} , 状态:{}",id , status == 1 ? "起售" : "停售" );
        dishService.setStatus(status, id);

        return Result.success();
    }
}
