package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DishServiceimpl implements DishService{

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;


     /**
     * 新增菜品  向菜品表插入数据并且向口味表里添加数据
     * @param dishDTO
     * @return
     */
    @Override
    @Transactional//事务注解,要么全成功要么全失败
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        //属性拷贝 , 向菜品表插入一条数据
        BeanUtils.copyProperties(dishDTO, dish);
        dish.setCreateTime(LocalDateTime.now());
        dish.setUpdateTime(LocalDateTime.now());
        dish.setUpdateUser(BaseContext.getCurrentId());
        dish.setCreateUser(BaseContext.getCurrentId());
        
        dishMapper.insert(dish);
        //获取insert语句生成的主键值
        Long dishId = dish.getId();;
        //获取dishDTO里的口味集合
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //向口味表里 批量插入n条数据(即插入集合对象)
            dishFlavorMapper.insertBatch(flavors);
        }



    }

}
