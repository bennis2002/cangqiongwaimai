package com.sky.service;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;

public interface DishService {
    Result saveWithFlavor(DishDTO dish);

    public void saveDish(Dish dish);

    PageResult PageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteByIds(Integer[] ids);

    DishVO queryById(Integer id);

    void updateDish(DishDTO dishVO);

    void updateFlavor(DishDTO dishDTO);
}
