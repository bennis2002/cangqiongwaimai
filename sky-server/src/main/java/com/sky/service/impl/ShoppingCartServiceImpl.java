package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Resource
    ShoppingCartMapper shoppingCartMapper;

    @Resource
    DishMapper dishMapper;

    @Resource
    SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO 购物车DTO
     */
    @Override
    @Transactional
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //判断当前加入到购物车中的商品是否已经存在了
        if (list != null && list.size() > 0) {
            //如果已经存在了，只需要将数量加一
            ShoppingCart shoppingCart1 = list.get(0);
            shoppingCart1.setNumber(shoppingCart1.getNumber() + 1);
            shoppingCartMapper.updateById(shoppingCart1);
        }
        else {
            //若未存在，则新建一个新的数据

            //判断本次添加到购物车的是菜品还是套餐(要么是菜品要么是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                Dish dish = dishMapper.selectById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
            } else {
                Setmeal setmeal = setmealMapper.selectById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long id = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, ShoppingCart::getUserId, id);
        List<ShoppingCart> list = shoppingCartMapper.selectList(lqw);
        return list;
    }

    @Override
    public void clean() {
        Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, ShoppingCart::getUserId, currentId);
        shoppingCartMapper.delete(lqw);
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        Long dishId = shoppingCartDTO.getDishId();
        String dishFlavor = shoppingCartDTO.getDishFlavor();
        Long setmealId = shoppingCartDTO.getSetmealId();

        lqw.eq(dishFlavor != null, ShoppingCart::getDishFlavor, dishFlavor)
                .eq(dishId != null, ShoppingCart::getDishId, dishId)
                .eq(setmealId != null, ShoppingCart::getSetmealId, setmealId);

        ShoppingCart shoppingCart = shoppingCartMapper.selectOne(lqw);
        Integer number = shoppingCart.getNumber();
        if(number > 1)
        {
            shoppingCart.setNumber(number - 1);
            shoppingCartMapper.updateById(shoppingCart);
        } else shoppingCartMapper.delete(lqw);
    }
}
