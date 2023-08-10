package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import io.swagger.models.auth.In;

import java.util.List;

public interface SetmealService {

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

    void saveSetmeal(SetmealDTO setmealDTO);

    SetmealVO queryById(Integer id);

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void deleteByIds(Integer[] ids);

    void updateById(SetmealDTO setmealDTO);

    void updateStatus(Integer status, Integer id);

    Setmeal BuildSetmeal(Setmeal setmeal);
}
