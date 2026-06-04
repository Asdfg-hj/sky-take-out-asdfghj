package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ShoppingCartServiceimpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long UserId = BaseContext.getCurrentId();
        shoppingCart.setUserId(UserId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //判断添加到购物车的商品是否已经存在
        //list里面最多一条数据
        if(!list.isEmpty()) {
                ShoppingCart cart = list.get(0);
                cart.setNumber(cart.getNumber() + 1);
                shoppingCartMapper.updateNumberById(cart);
            // 如果已经存在,只需要将数量加一即可
        } else {
            // 如果不存在,则添加到购物车,考虑添加的是菜品还是套餐
            //需要考虑冗余字段name image price,减少后续查询的复杂度,用空间换时间
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null) {
            //添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCartMapper.insert(shoppingCart);
            } else {
            //添加到购物车的是套餐  
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCartMapper.insert(shoppingCart);
            }
            shoppingCart.setNumber(1);
                shoppingCart.setCreateTime(LocalDateTime.now());
        }
    }
}

