package com.sky.service;

import java.util.List;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

public interface SetmealService {

    /**
     * 新增套餐
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);
    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);
    /**
     * 批量删除套餐
     * @param ids
     * @return
     */
    void deleteBatch(List<Long> ids);
    /**
     * 启售停售套餐
     * @param status
     */
    void startOrStop(Integer status,Long id);
    /**
     * 根据id查询套餐和关联的菜品数据
     * @param id
     * @return
    */
    SetmealVO getByIdWithDish(Long id);

    /**
         * 修改套餐
         * @param setmealDTO
    */
    void update(SetmealDTO setmealDTO);
    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);
    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);
}
