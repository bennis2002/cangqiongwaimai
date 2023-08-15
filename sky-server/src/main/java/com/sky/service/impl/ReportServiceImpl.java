package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Resource
    OrderMapper orderMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    OrderDetailMapper orderDetailMapper;
    @Resource
    WorkspaceService workspaceService;
    
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDatalist(begin, end);
        List<BigDecimal> sumList = new ArrayList<>();

        for (LocalDate item : dateList) {
            LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
            lqw.eq(true, Orders::getStatus, 5)
                    .ge(true, Orders::getOrderTime, LocalDateTime.of(item, LocalTime.MIN))
                    .le(true, Orders::getOrderTime, LocalDateTime.of(item, LocalTime.MAX));
            List<Orders> orders = orderMapper.selectList(lqw);
            BigDecimal sum = BigDecimal.valueOf(0);
            for (Orders order : orders) {
                sum = sum.add(order.getAmount());
            }
            sumList.add(sum);
        }

        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        turnoverReportVO.setDateList(StringUtils.join(dateList , ","));
        turnoverReportVO.setTurnoverList(StringUtils.join(sumList , ","));
        
        return turnoverReportVO;
    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDatalist(begin, end);
        List<Integer> totaluserList = new ArrayList<>();
        List<Integer> newuserList = new ArrayList<>();

        for (LocalDate localDate : dateList) {
            List<User> newUsers = userMapper.selectList(new LambdaQueryWrapper<User>().ge(true, User::getCreateTime, LocalDateTime.of(localDate, LocalTime.MIN))
                    .le(true, User::getCreateTime, LocalDateTime.of(localDate, LocalTime.MAX)));
            newuserList.add(newuserList.size());

            List<User> totalUsers = userMapper.selectList(new LambdaQueryWrapper<User>().le(true, User::getCreateTime, LocalDateTime.of(localDate, LocalTime.MAX)));
            totaluserList.add(totalUsers.size());
        }


        UserReportVO userReportVO = new UserReportVO();
        userReportVO.setDateList(StringUtils.join(dateList , ","));
        userReportVO.setNewUserList(StringUtils.join(newuserList , ","));
        userReportVO.setTotalUserList(StringUtils.join(totaluserList , ","));

        return userReportVO;
    }

    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> list = getDatalist(begin, end);
        List<Integer> totalOrdersList = new ArrayList<>();
        List<Integer> finishOrderList = new ArrayList<>();
        OrderReportVO orderReportVO = new OrderReportVO();

        for (LocalDate localDate : list) {
            //查每天的订单数
            List<Orders> totalOrders = orderMapper.selectList(new LambdaQueryWrapper<Orders>().ge(true, Orders::getOrderTime, LocalDateTime.of(localDate, LocalTime.MIN))
                    .le(true, Orders::getOrderTime, LocalDateTime.of(localDate, LocalTime.MAX)));

            List<Orders> FinishOrders = orderMapper.selectList(new LambdaQueryWrapper<Orders>().ge(true, Orders::getOrderTime, LocalDateTime.of(localDate, LocalTime.MIN))
                    .le(true, Orders::getOrderTime, LocalDateTime.of(localDate, LocalTime.MAX))
                    .eq(true, Orders::getStatus, 5));
            totalOrdersList.add(totalOrders.size());
            finishOrderList.add(FinishOrders.size());
        }

        //订单总数 跟 有效订单总数
        Integer totalSum = totalOrdersList.stream().reduce(Integer::sum).get();
        Integer finish = finishOrderList.stream().reduce(Integer::sum).get();

        double Rate = 0.0;
        if(totalSum != 0)  Rate = finish.doubleValue() / totalSum;


        orderReportVO.setDateList(StringUtils.join(list , ","));
        orderReportVO.setOrderCountList(StringUtils.join(totalOrdersList , ","));
        orderReportVO.setValidOrderCountList(StringUtils.join(finishOrderList , ","));
        orderReportVO.setOrderCompletionRate(Rate);
        orderReportVO.setValidOrderCount(finish);
        orderReportVO.setTotalOrderCount(totalSum);

        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesDTOS = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesDTOS.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesDTOS.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        String numberList = StringUtils.join(numbers, ",");

        SalesTop10ReportVO salesTop10ReportVO = new SalesTop10ReportVO();
        salesTop10ReportVO.setNameList(nameList);
        salesTop10ReportVO.setNumberList(numberList);

        return salesTop10ReportVO;
    }

    @Override
    public void export(HttpServletResponse response) {
        //1. 查询数据库
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));
        //2. 通过POI将数据下载到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //填充数据
            XSSFSheet sheetAt = excel.getSheetAt(0);

            sheetAt.getRow(1).getCell(1).setCellValue("时间 : " + dateBegin + "至" + dateEnd);

            XSSFRow row = sheetAt.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());

            row = sheetAt.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            for (int i = 0; i < 30; i++) {
                LocalDate date = dateEnd.minusDays(i);
                //查询某一天的营业额
                BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获取某一行
                row = sheetAt.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());

                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            }

            //3. 通过数据流下载到用户端
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }




    }

    public List<LocalDate> getDatalist(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate a = begin;
        dateList.add(a);
        while (!a.equals(end)) {
            a = a.plusDays(1);
            dateList.add(a);
        }
        return dateList;
    }

}
