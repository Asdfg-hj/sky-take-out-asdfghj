package com.sky.service;

import java.util.List;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

public interface DishService {

     /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    public void saveWithFlavor(DishDTO dishDTO);


    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品的批量删除
     * @param ids
     */
    public void deleteBatch(List<Long> ids);

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id);

    /**
     * 根据id修改菜品信息和对应的口味信息
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO);

    /**
     * 根据分类id查询菜品
     * @param dishDTO
     * @return
     */
    public List<DishVO> listWithFlavor(Long categoryId);
    /**
     * 启用禁用菜品
     * @param status
     * @param id
     */
    public void startOrStop(Integer status,Long id);
     /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);
}
