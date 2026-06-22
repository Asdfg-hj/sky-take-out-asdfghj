package com.sky.service;

import java.time.LocalDate;

import javax.servlet.http.HttpServletResponse;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

public interface ReportService {

    /**
     * 统计指定时间区间内的营业额统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin,LocalDate end);
    /**
     * 统计指定时间区间内的用户统计
     * @param begin
     * @param end
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin,LocalDate end);
    /**
     * 统计指定时间区间内的订单统计
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO getOrdersStatistics(LocalDate begin,LocalDate end);
    /**
     * 统计指定时间区间内的销量排行top10
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO getSalesTop10Statistics(LocalDate begin,LocalDate end);
    /**
     * 导出运营数据excel报表
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
