package com.sky.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.github.pagehelper.Page;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.vo.SetmealVO;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Integer id);

    /**
     * 新增套餐 使用xml
     * @param setmeal
     */
     //这里需要返回 插入后可以拿到自增主键（setmeal.getId() 就有值了）
    void insert(Setmeal setmeal);
    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);
    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(Long id);
    /**
     * 批量删除套餐表里的套餐
     * @param ids
     */
     @Delete("delete from setmeal where id = #{id}")
    void deleteById(Long setmealId); 
    /**
     * 更新套餐信息
     * @param setmeal
     */
    void update(Setmeal setmeal);
}

