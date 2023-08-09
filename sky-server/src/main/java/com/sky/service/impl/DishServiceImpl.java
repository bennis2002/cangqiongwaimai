package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Resource
    DishService dishService;

    @Resource
    DishMapper dishMapper;

    @Resource
    DishFlavorMapper dishFlavorMapper;

    @Override
    @Transactional
    public Result saveWithFlavor(DishDTO dish) {

        //向菜品表中插入一条数据
        Dish dish1 = new Dish();
        BeanUtils.copyProperties(dish, dish1);
        dishService.saveDish(dish1);


        List<DishFlavor> flavors = dish.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            //向口味表插入n条数据
            for (DishFlavor item : flavors) {
                item.setDishId(dish1.getId());
                dishFlavorMapper.insert(item);
            }
        }
        return Result.success("插入成功");
    }

    @Transactional
    @AutoFill(OperationType.INSERT)
    public void saveDish(Dish dish) {
        dishMapper.insert(dish);
    }

    @Override
    public PageResult PageQuery(DishPageQueryDTO dishPageQueryDTO) {

        dishPageQueryDTO.setPage(dishPageQueryDTO.getPage() * dishPageQueryDTO.getPageSize() - dishPageQueryDTO.getPageSize());

        List<DishVO> result = dishMapper.seleceByPage(dishPageQueryDTO);
        PageResult pageResult = new PageResult();

        pageResult.setRecords(result);
        pageResult.setTotal(result.size());

        return pageResult;
    }

    @Override
    @Transactional
    public void deleteByIds(Integer[] ids) {
        dishMapper.deleteBatchIds(Arrays.asList(ids));

        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();

        lqw.in(DishFlavor::getDishId, ids);

        dishFlavorMapper.delete(lqw);
    }

    @Override
    public DishVO queryById(Integer id) {
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, Dish::getId, id);

        Dish dish = dishMapper.selectOne(lqw);

        DishVO dishDTO = new DishVO();

        BeanUtils.copyProperties(dish, dishDTO);

        LambdaQueryWrapper<DishFlavor> lqw1 = new LambdaQueryWrapper<>();
        lqw1.eq(true, DishFlavor::getDishId, id);

        dishDTO.setFlavors(dishFlavorMapper.selectList(lqw1));

        return dishDTO;
    }

    @Transactional
    @Override
    public void updateDish(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, Dish::getId, dishDTO.getId());

        dishMapper.update(dish, lqw);

        dishService.updateFlavor(dishDTO);
    }

    @Override
    @Transactional
    public void updateFlavor(DishDTO dishDTO) {
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, DishFlavor::getDishId, dishDTO.getId());

        dishFlavorMapper.delete(lqw);

        List<DishFlavor> list = dishDTO.getFlavors();
        for (DishFlavor item : list) {
            item.setDishId(dishDTO.getId());
            dishFlavorMapper.insert(item);
        }
    }

}