package com.sky.service.impl;



import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReportServiceimpl implements ReportService{

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 统计指定时间区间内的营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end范围内的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算:计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList ){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover; 
            turnoverList.add(turnover);
        }
        return TurnoverReportVO.builder()
                        .dateList(StringUtils.join(dateList, ","))
                        .turnoverList(StringUtils.join(turnoverList, ","))
                        .build();
    }
    /**
     * 统计指定时间区间内的用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 截至当天结束的总用户数
            Map<String, Object> totalMap = new HashMap<>();
            totalMap.put("end", endTime);
            Integer totalUser = userMapper.countByMap(totalMap);
            totalUserList.add(totalUser == null ? 0 : totalUser);

            // 当天新增用户数
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            Map<String, Object> newMap = new HashMap<>();
            newMap.put("begin", beginTime);
            newMap.put("end", endTime);
            Integer newUser = userMapper.countByMap(newMap);
            newUserList.add(newUser == null ? 0 : newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }
    /**
     * 统计指定时间区间内的订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(! begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //计算每日订单总数
        List<Integer> orderCountList = new ArrayList<>();
        //计算每日有效订单总数
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer orderCount = orderMapper.countByMap(map);
            orderCountList.add(orderCount);
            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(map);
            validOrderCountList.add(validOrderCount);
        }
        //计算时间区间内的订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //计算时间区间内的有效订单总数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount * 1.0  / totalOrderCount;
        }
        
        return OrderReportVO.builder()
                    .dateList(StringUtils.join(dateList,","))
                    .orderCountList(StringUtils.join(orderCountList,","))
                    .validOrderCountList(StringUtils.join(validOrderCountList,","))
                    .totalOrderCount(totalOrderCount)
                    .validOrderCount(validOrderCount)
                    .orderCompletionRate(orderCompletionRate)
                    .build();
    }
    /**
     * 统计指定时间区间内的销量排行top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10Statistics(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime,endTime);
        //也可以用for循环
        List<String> names =  salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        return SalesTop10ReportVO.builder()
                    .nameList(StringUtils.join(names,","))
                    .numberList(StringUtils.join(numbers,","))
                    .build();
    }
    /**
     * 导出运营数据excel报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库，获取营业数据
        LocalDate dateBegin = LocalDate.now().plusDays(-30);
        LocalDate dateEnd = LocalDate.now().plusDays(-1);
        LocalDateTime beginTime = LocalDateTime.of(dateBegin,LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(dateEnd,LocalTime.MAX);
        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);


        //2.通过POI将数据写到excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        //基于模板文件创建一个新的excel文件
        try {
			XSSFWorkbook excel = new XSSFWorkbook(in);
            //填充表格文件的sheet页
            XSSFSheet sheet = excel.getSheet("sheet1");
            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间："+dateBegin+"至"+dateEnd);
            //获得第四行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            //获得第五行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());
            //填充明细数据
            for(int i = 0; i < 30;i++ ){
                LocalDate date = dateBegin.plusDays(i);
                //查询某天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN),LocalDateTime.of(date,LocalTime.MAX) );
                row = sheet.getRow((i + 7));
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }
            
            //3.通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            out.close();
            excel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

}