package com.sky.controller.admin;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate<String, List<DishVO>> redisTemplate;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result<?> save(@RequestBody DishDTO dishDTO){

        log.info("新增菜品,{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);
        //清理redis中菜品数据,因为新增了菜品,所以之前的缓存数据就过期了,要清理掉
        cleanCache("dish_*");
        return Result.success();
    }
    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询,{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);

        return Result.success(pageResult);
    }

    /**
     * 菜品的批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    //@RequestParam  解析传过来的字符串,并封装在ids内
    public Result<?> delete(@RequestParam List<Long> ids){

        log.info("菜品的批量删除,{}",ids);
        dishService.deleteBatch(ids);
        //将所有的菜品缓存数据清理掉
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){

        log.info("根据id查询菜品,{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);;
        return Result.success(dishVO);

    }
    /**
     * 根据id修改菜品信息和对应的口味信息
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result<?> update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品,{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);
        //清理redis中菜品数据,因为修改了菜品,所以之前的缓存数据就过期了,要清理掉
        cleanCache("dish_*");
        return Result.success();


    }
    /**
     * 根据分类id查询菜品
     * @param dishDTO
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId){
        //构造redis的key,格式为: dish_分类id
        String key = "dish_" + categoryId;
        //查询reids中是否有数据 
        // 频繁访问数据库会导致性能问题,所以先查询redis，用到了缓存
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        // 如果有直接返回
        if(list != null && list.size() > 0){
            return Result.success(list);
        }
        // 如果没有，再查询数据库,并将数据存入redis
        log.info("根据分类id查询菜品,{}",categoryId);
        list = dishService.listWithFlavor(categoryId);
        redisTemplate.opsForValue().set(key, list);
        return Result.success(list);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用菜品")
    public Result<?> startOrStop(@PathVariable Integer status, Long id) {
    dishService.startOrStop(status, id);
    //清理redis中菜品数据,因为修改了菜品状态,所以之前的缓存数据就过期了,要清理掉
    cleanCache("dish_*");
    return Result.success();
    }
    /**
     * 清理redis中菜品数据
      * @param pattern
     */
    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
