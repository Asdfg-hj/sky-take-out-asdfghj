package com.sky.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
//import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Select;

import com.sky.entity.SetmealDish;

@Mapper
public interface SetmealDishMapper {

    /**
     * 动态SQL 传进来的是list集合
     * 根据菜品id来查询对应的套餐id
     * @param dishIds
     * @return
     */
    //select setmeal_id from setmeal_dish where dish_id in (1,2,3)
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 将套餐里的菜品集合插入到套餐菜品表
     * @param setmealDishs
     */
    void insertBatch(List<SetmealDish> setmealDishes);
    /**
     * 根据套餐id批量删除套餐菜品表里的菜品
     * @param setmealId
     */
     @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId); 
    /**
     * 根据套餐id查询菜品集合
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);
}
