package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.annotation.AutoFill;
import com.sky.enumeration.OperationType;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {

    /**
     * 动态条件查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

	/**
     * 根据套餐id查询菜品选项
     * @param setmealId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);

    List<SetmealVO> selectByPage(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据条件统计套餐数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
