package com.sky.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.sky.entity.ShoppingCart;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 动态条件查询购物车列表
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);
    /**
     * 根据id修改购物车商品数据
     * @param shoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);
    /**
     * 插入购物车数据
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);
    /**
     * 根据用户id清空购物车
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);
    /**
     * 将购物车数据插入到数据库
     * @param shoppingCartList
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
