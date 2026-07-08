package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import com.sky.service.AiService;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceimpl implements DishService{

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private AiService aiService;                  // ← 新增：注入 AI 服务
    @Autowired
    private CategoryMapper categoryMapper;   // ← 新增：需要查分类名
    
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
        // ========== 🆕 新增：如果描述为空，调用 AI 生成 ==========
        if (dish.getDescription() == null || dish.getDescription().trim().isEmpty()) {
            try {
                // 1. 根据分类 ID 查分类名
                String categoryName = "中式"; // 默认值
                if (dish.getCategoryId() != null) {
                    Category category = categoryMapper.getById(dish.getCategoryId());
                    if (category != null) {
                        categoryName = category.getName();
                    }
                }
                
                // 2. 获取口味信息
                String flavorInfo = "";
                if (dishDTO.getFlavors() != null && !dishDTO.getFlavors().isEmpty()) {
                    flavorInfo = dishDTO.getFlavors().stream()
                            .map(f -> f.getName() + "可选（" + f.getValue() + "）")
                            .collect(Collectors.joining("、"));
                }

                // 3. 调用 AI 生成描述
                String aiDescription = aiService.generateDishDescription(
                        dish.getName(),
                        categoryName,
                        flavorInfo
                );
                
                // 4. 设置到 dish 对象
                dish.setDescription(aiDescription);
                log.info("AI 生成描述成功：{} → {}", dish.getName(), aiDescription);
                
            } catch (Exception e) {
                // AI 调用失败不影响主流程，记录日志即可
                log.error("AI 生成描述失败，使用空描述", e);
                dish.setDescription(""); // 或者保持原样
            }
        }
        // ========================================================
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
    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
     @Override
     public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {

        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        Long total = page.getTotal();
        List<DishVO> records = page.getResult();


        return new PageResult(total,records);
        
     }
     /**
     * 菜品的批量删除
     * @param ids
     */
    @Override
    @Transactional//保证事务的一致性
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品能否删除,当前菜品是否在起售中
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                //当前菜品处于起售中,不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            
        }
        //判断当前菜品能否删除,当前菜品是否与套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0){
            //当前菜品与套餐关联,不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        
        /* //删除菜品表中的菜品数据
        for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品口味表中 与菜品关联的口味的数据
            dishFlavorMapper.deleteByDishId(id);
        }
         */

        //根据菜品id集合批量删除菜品数据
        dishMapper.deleteByIds(ids);
        //根据菜品id集合批量删除关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
     @Override
     public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品表里的基本数据
        Dish dish = dishMapper.getById(id);
        //根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //将查询到的数据封装到DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
     }
     /**
     * 根据id修改菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {

        //修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dish.setUpdateTime(LocalDateTime.now());
        dish.setUpdateUser(BaseContext.getCurrentId());

        dishMapper.update(dish);
        //删除菜品口味表的原有数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //重新插入新数据到菜品口味表
        List<DishFlavor> flavors = dishDTO.getFlavors();
       if(flavors != null && flavors.size() > 0){
            
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //向口味表里 批量插入n条数据(即插入集合对象)
            dishFlavorMapper.insertBatch(flavors);
         }
    }
    /**
     * 根据分类id查询菜品
     * @param dishDTO
     * @return
     */
     @Override
     public List<DishVO> listWithFlavor(Long categoryId) {
        
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        return dishMapper.listWithFlavor(dish);
        
     }
     /**
      * 启用禁用菜品
      */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
            .id(id)
            .status(status)
            .updateTime(LocalDateTime.now())
            .updateUser(BaseContext.getCurrentId())
            .build();
        dishMapper.update(dish);
    }    
    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<DishVO> dishList = dishMapper.listWithFlavor(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (DishVO d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

}
