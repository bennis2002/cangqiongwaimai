package com.sky.service;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    Result saveWithFlavor(DishDTO dish);

    public void saveDish(Dish dish);

    PageResult PageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteByIds(Integer[] ids);

    DishVO queryById(Integer id);

    void updateDish(DishDTO dishVO);

    void updateFlavor(DishDTO dishDTO);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    List<Dish> queryByCategoryId(Integer categoryId);
}
