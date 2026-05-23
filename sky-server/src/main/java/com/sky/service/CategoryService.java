package com.sky.service;


import java.util.List;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

public interface CategoryService {

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);


    /**
     * 修改分类
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO);

    /**
     * 启用禁用分类
     * @param id
     */
    public void startOrStop(Integer status,Long id);

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    public void insert(CategoryDTO categoryDTO);

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteById(Integer id);

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    List<Category> list(Integer type);

}
