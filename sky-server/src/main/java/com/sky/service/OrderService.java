package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.*;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    OrderVO orderDetail(Integer id);

    PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    void cancel(Long id);

    void repetition(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    OrderVO detail(Long id);

    void confirm(Long id);

    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) throws Exception;

    void delivery(Long id);

    void complete(Long id);

    void remind(Long id);
}
