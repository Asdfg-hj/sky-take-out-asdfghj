package com.sky.controller.admin;


import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Slf4j
@Api(tags = "分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 分页查询分类
     */
    @GetMapping("/page")
    @ApiOperation("分类分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){

        log.info("分类分页查询,参数为:{}",categoryPageQueryDTO);
        PageResult pageResult =  categoryService.pageQuery(categoryPageQueryDTO);

        return Result.success(pageResult);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    @PutMapping
    @ApiOperation("修改分类")
    public Result<?> update(@RequestBody CategoryDTO categoryDTO){
        log.info("修改分类,参数为：{}",categoryDTO);
        categoryService.update(categoryDTO);

        return Result.success();

    }
    /**
     * 启用禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用分类")
    public Result<?> startOrStop(@PathVariable Integer status,Long id){

        log.info("启用或禁用分类,参数为：{},{}",status,id);
        categoryService.startOrStop(status,id);
        return Result.success();


    }

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增分类")
    public Result<String> insert(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类,参数为：{}",categoryDTO);
        categoryService.insert(categoryDTO);
        return Result.success();

    }
    /**
     * 根据id删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据id删除分类")
    public Result<?> deleteById(Integer id){
        log.info("删除分类,参数为:{}",id);
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据类型查询分类")
    public Result<?> selectByCategory(Integer type){
        log.info("根据类型查询分类,参数为:{}",type);
        List<Category> list = categoryService.list(type);
        return Result.success(list);

    }

}
