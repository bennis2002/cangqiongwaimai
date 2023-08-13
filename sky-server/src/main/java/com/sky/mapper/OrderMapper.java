package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    List<Orders> selectAll(int status);

    @Select("select * from orders where status = #{pendingPayment} and order_time < #{localDateTime} ")
    List<Orders> getByStatusAndTimeLT(Integer pendingPayment, LocalDateTime localDateTime);
}
