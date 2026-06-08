package com.sky.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.sky.entity.OrderDetail;

@Mapper
public interface OrderDetailMapper {

    void insertBatch(List<OrderDetail> orderDetailList);

}
