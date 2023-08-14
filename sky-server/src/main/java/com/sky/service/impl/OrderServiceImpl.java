package com.sky.service.impl;


import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.Websocket.WebSocketServer;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.sky.entity.Orders.*;

@Slf4j
@Service
@Api("订单管理")
public class OrderServiceImpl implements OrderService {
    @Resource
    OrderMapper orderMapper;
    @Resource
    OrderDetailMapper orderDetailMapper;
    @Resource
    ShoppingCartMapper shoppingCartMapper;
    @Resource
    AddressBookMapper addressBookMapper;
    @Resource
    WeChatPayUtil weChatPayUtil;
    @Resource
    UserMapper userMapper;
    @Resource
    WebSocketServer webSocketServer;


    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        Long id = BaseContext.getCurrentId();

        //做一些校验性性工作（处理各种业务异常）【地址簿为空 ， 购物车为空】
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook AddressData = addressBookMapper.getById(addressBookId);
        if (AddressData == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(true, ShoppingCart::getUserId, id);
        List<ShoppingCart> list = shoppingCartMapper.selectList(shoppingCartLambdaQueryWrapper);
        if (list == null || list.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入一条数据
        orders.setUserId(id);
        orders.setOrderTime(LocalDateTime.now());
        orders.setStatus(PENDING_PAYMENT);
        orders.setPayStatus(UN_PAID);
        //以下数据可以用Copy优化
        orders.setAddress(AddressData.getDetail());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setConsignee(AddressData.getConsignee());
        orders.setPhone(AddressData.getPhone());

        orderMapper.insert(orders);

        Long ordersId = orders.getId();
        //向订单明细表插入n调数据
        for (ShoppingCart item : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(item, orderDetail);
            orderDetail.setOrderId(ordersId);
            orderDetailMapper.insert(orderDetail);
        }

        //删除购物车中的相关数据
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, ShoppingCart::getUserId, id);
        shoppingCartMapper.delete(lqw);

        //封装VO的返回数据
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        BeanUtils.copyProperties(orders, orderSubmitVO);

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        LambdaQueryWrapper<Orders> lqw1 = new LambdaQueryWrapper<>();
        lqw1.eq(true, Orders::getNumber, outTradeNo);
        Orders ordersDB = orderMapper.selectOne(lqw1);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(true, Orders::getId, outTradeNo);
        orderMapper.update(orders, lqw);

        //通过websocket发送消息
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", orders.getId());
        map.put("content", "订单号" + outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    @Override
    public OrderVO orderDetail(Integer id) {
        Orders item =orderMapper.selectById(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(item , orderVO);

        LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderDetailLambdaQueryWrapper.eq(true, OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(orderDetailLambdaQueryWrapper);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        List<OrderVO> orderVOS = new ArrayList<>();

        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        Integer status = ordersPageQueryDTO.getStatus();
        lqw.eq(status != null, Orders::getStatus, status).orderByDesc(Orders::getOrderTime);

        IPage ipage = new Page(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        IPage result = orderMapper.selectPage(ipage, lqw);
        List records = result.getRecords();

        for (Object item : records) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(item, orderVO);

            Long id = orderVO.getId();
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(true, OrderDetail::getOrderId, id);
            List<OrderDetail> orderDetails = orderDetailMapper.selectList(orderDetailLambdaQueryWrapper);
            orderVO.setOrderDetailList(orderDetails);
            orderVOS.add(orderVO);
        }
        PageResult pageResult = new PageResult();
        pageResult.setRecords(orderVOS);
        pageResult.setTotal(result.getTotal());
        return pageResult;
    }

    @Override
    public void cancel(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.CANCELLED);
        orderMapper.updateById(orders);
    }

    @Override
    @Transactional
    public void repetition(Long id) {
        Orders orders = orderMapper.selectById(id);
        Long userId = orders.getUserId();
        LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderDetailLambdaQueryWrapper.eq(true, OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(orderDetailLambdaQueryWrapper);
        for (OrderDetail item : orderDetails) {
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            BeanUtils.copyProperties(item, shoppingCart);
            shoppingCart.setId(null);
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = new PageResult();

        IPage ipage = new Page<Orders>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();

        String phone = ordersPageQueryDTO.getPhone();
        Integer status = ordersPageQueryDTO.getStatus();
        String number = ordersPageQueryDTO.getNumber();
        LocalDateTime beginTime = ordersPageQueryDTO.getBeginTime();
        LocalDateTime endTime = ordersPageQueryDTO.getEndTime();

        lqw.like(phone != null, Orders::getPhone, phone)
                .like(number != null, Orders::getNumber, number)
                .eq(status != null, Orders::getStatus, status)
                .ge(beginTime != null, Orders::getOrderTime, beginTime)
                .le(endTime != null, Orders::getOrderTime, endTime)
                .orderByDesc(Orders::getOrderTime);

        IPage result = orderMapper.selectPage(ipage, lqw);
        List<OrderVO> list = new ArrayList<>();

        for (Object item : result.getRecords()) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(item, orderVO);
            Long id = orderVO.getId();
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(true, OrderDetail::getOrderId, id);

            List<OrderDetail> orderDetails = orderDetailMapper.selectList(orderDetailLambdaQueryWrapper);
            String a = "";
            for (OrderDetail detail : orderDetails) {
                if (a.equals("")) {
                    a += detail.getName();
                } else a += "," + detail.getName() ;
            }
            orderVO.setOrderDishes(a);
            if(orderVO.getCancelReason() == null) orderVO.setCancelReason(orderVO.getRejectionReason());
            list.add(orderVO);
        }
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(list);

        return pageResult;
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(orderMapper.selectAll(Orders.CONFIRMED).size());
        orderStatisticsVO.setToBeConfirmed(orderMapper.selectAll(Orders.TO_BE_CONFIRMED).size());
        orderStatisticsVO.setDeliveryInProgress(orderMapper.selectAll(DELIVERY_IN_PROGRESS).size());

        return orderStatisticsVO;
    }

    @Override
    public OrderVO detail(Long id) {
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orderMapper.selectById(id), orderVO);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(true, OrderDetail::getOrderId, id));
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    @Override
    public void confirm(Long id) {
        Orders orders = orderMapper.selectById(id);
        Integer status = orders.getStatus();
        if (status == 2) {
            orders.setStatus(++status);
            orderMapper.updateById(orders);
        } else {
            throw new OrderBusinessException("订单状态异常");
        }
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders orders = orderMapper.selectById(ordersRejectionDTO.getId());
        Integer status = orders.getStatus();
        if (status == 2) {
            orders.setStatus(6);
            orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
            orders.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(orders);
        } else {
            throw new OrderBusinessException("订单状态异常");
        }
        //支付状态
        Integer payStatus = orders.getPayStatus();
        if (Objects.equals(payStatus, PAID)) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));
            log.info("申请退款：{}", refund);
        }
    }

    @Override
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders orders = orderMapper.selectById(ordersCancelDTO.getId());
        Integer status = orders.getStatus();
        if (status != null) {
            orders.setStatus(6);
            orders.setCancelReason(ordersCancelDTO.getCancelReason());
            orders.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(orders);
        } else {
            throw new OrderBusinessException("订单状态异常");
        }
        //支付状态
        Integer payStatus = orders.getPayStatus();
        if (Objects.equals(payStatus, PAID)) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));
            log.info("申请退款：{}", refund);
        }
    }

    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (orders.getStatus() == 3) {
            orders.setStatus(4);
            orders.setEstimatedDeliveryTime(LocalDateTime.now().plusHours(1));
            orderMapper.updateById(orders);
        } else throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (orders.getStatus() == 4) {
            orders.setDeliveryTime(LocalDateTime.now());
            orders.setStatus(COMPLETED);
            orderMapper.updateById(orders);
        } else throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    @Override
    public void remind(Long id) {
        //查询订单
        Orders orders = orderMapper.selectById(id);
        if (orders == null) {
            throw new OrderBusinessException((MessageConstant.ORDER_NOT_FOUND));
        }

        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号: "+ orders.getNumber());

        //发送请求
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Override
    public void test(Long id) {
        Orders orders = orderMapper.selectById(id);
        String s = RandomUtil.randomNumbers(10);
        orders.setNumber(s);
        orders.setStatus(2);
        orderMapper.updateById(orders);
        paySuccess(s);
    }


}
