package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Resource
    private SetmealMapper setmealMapper;
    @Resource
    private SetmealDishMapper setmealDishMapper;
    @Resource
    private SetmealService setmealService;

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    @Override
    @Transactional
    public void saveSetmeal(SetmealDTO setmealDTO) {
        log.info("新增套餐");

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmeal = setmealService.BuildSetmeal(setmeal);

        //插入主要信息
        setmealMapper.insert(setmeal);

        //插入菜品信息
        for (SetmealDish item : setmealDTO.getSetmealDishes()) {
            item.setSetmealId(setmeal.getId());
            setmealDishMapper.insert(item);
        }
    }

    @AutoFill(OperationType.INSERT)
    public Setmeal BuildSetmeal(Setmeal setmeal) {
        return setmeal;
    }

    @Override
    public SetmealVO queryById(Integer id) {
        log.info("根据ID查询套餐信息");
        SetmealVO setmealVO = new SetmealVO();

        //查主表
        Setmeal setmeal = setmealMapper.selectById(id);
        BeanUtils.copyProperties(setmeal, setmealVO);

        //查对应菜品中间表
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, SetmealDish::getSetmealId, id);

        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(lqw);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageResult pageResult = new PageResult();

        setmealPageQueryDTO.setPage(setmealPageQueryDTO.getPage() * setmealPageQueryDTO.getPageSize() - setmealPageQueryDTO.getPageSize());

        List<SetmealVO> list = setmealMapper.selectByPage(setmealPageQueryDTO);

        pageResult.setTotal(list.size());
        pageResult.setRecords(list);

        return pageResult;
    }

    @Override
    public void deleteByIds(Integer[] ids) {
        setmealMapper.deleteBatchIds(Arrays.asList(ids));
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.in(ids != null, SetmealDish::getDishId, ids);
        setmealDishMapper.delete(lqw);
    }

    @Override
    public void updateById(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmeal = setmealService.BuildSetmeal(setmeal);
        setmealMapper.updateById(setmeal);
    }

    @Override
    public void updateStatus(Integer status, Integer id) {
        Setmeal setmeal = Setmeal.builder()
                .id(Long.valueOf(id))
                .status(status)
                .build();

        setmealMapper.updateById(setmeal);
    }
}
