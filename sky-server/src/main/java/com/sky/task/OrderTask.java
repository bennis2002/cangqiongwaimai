package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Resource
    OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrder() {
        log.info("定时处理超时订单 : {}", LocalDateTime.now());

        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-15);

        List<Orders> TLlist = orderMapper.getByStatusAndTimeLT(Orders.PENDING_PAYMENT, localDateTime);

        if (TLlist != null && TLlist.size() != 0) {
            for (Orders TLItem : TLlist) {
                TLItem.setStatus(Orders.CANCELLED);
                TLItem.setCancelReason("订单超时，自动取消");
                TLItem.setCancelTime(LocalDateTime.now());
                orderMapper.updateById(TLItem);
            }
        }
    }

    /***
     * 处理在派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("处理派送中的订单 ： {}", LocalDateTime.now());
        List<Orders> list = orderMapper.getByStatusAndTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));
        if (list != null && list.size() != 0) {
            for (Orders item : list) {
                item.setStatus(Orders.COMPLETED);
                item.setCancelTime(LocalDateTime.now());
                orderMapper.updateById(item);
            }
        }
    }
}
