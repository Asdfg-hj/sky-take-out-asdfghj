package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Update;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.AiService;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SetmealServiceimpl implements SetmealService{
    
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private AiService aiService;
    @Autowired
    private CategoryMapper categoryMapper;
    /**
     * 新增套餐,同时需要保存套餐和菜品的关联关系
     */
    @Transactional
    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {

        //将新增套餐插入到套餐表
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmeal.setStatus(StatusConstant.DISABLE); 
        setmeal.setCreateTime(LocalDateTime.now());
        setmeal.setUpdateTime(LocalDateTime.now());
        setmeal.setUpdateUser(BaseContext.getCurrentId());
        setmeal.setCreateUser(BaseContext.getCurrentId());

        // 如果描述为空，调用 AI 生成
        if (setmeal.getDescription() == null || setmeal.getDescription().trim().isEmpty()) {
            try {
                // 1. 查分类名
                String categoryName = "中式";
                if (setmeal.getCategoryId() != null) {
                    Category category = categoryMapper.getById(setmeal.getCategoryId());
                    if (category != null) {
                        categoryName = category.getName();
                    }
                }

                // 2. 构建套餐内菜品列表信息
                String dishListInfo = "";
                List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
                if (setmealDishes != null && !setmealDishes.isEmpty()) {
                    dishListInfo = setmealDishes.stream()
                            .map(d -> d.getName() + " x" + d.getCopies())
                            .collect(java.util.stream.Collectors.joining("、"));
                }

                // 3. 调用 AI 生成描述
                String aiDescription = aiService.generateSetmealDescription(
                        setmeal.getName(),
                        categoryName,
                        dishListInfo
                );

                setmeal.setDescription(aiDescription);
                log.info("AI 生成套餐描述成功：{} → {}", setmeal.getName(), aiDescription);

            } catch (Exception e) {
                log.error("AI 生成套餐描述失败，使用空描述", e);
                setmeal.setDescription("");
            }
        }

        setmealMapper.insert(setmeal);

        //将套餐与菜品根据套餐id关联起来
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //获取生成的套餐id
        Long setmealId = setmeal.getId();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        //将新增的套餐菜品 插入到 套餐菜品表 里面
        setmealDishMapper.insertBatch(setmealDishes);
    }
    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        Long total = page.getTotal();
        List<SetmealVO> records = page.getResult();
        return new PageResult(total,records);    
    }
    /**
     * 批量删除套餐,起售中的套餐不能删除
     * 删除表中的套餐以及删除套餐菜品表里的数据
     * @param ids
     * @return
     */
    @Transactional
    public void deleteBatch(List<Long> ids){
        ids.forEach(id ->{
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(setmealId -> {
            //删除套餐表中的数据
            setmealMapper.deleteById(setmealId);
            //删除套餐菜品关系表中的数据
            setmealDishMapper.deleteBySetmealId(setmealId); 
    });
    }
    /**
     * 启售停售套餐
     * @param status
     */
    @Override
    public void startOrStop(Integer status,Long id) {
        if(status == StatusConstant.ENABLE){
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            dishList.forEach(dish->{
                if(dish.getStatus() == StatusConstant.DISABLE){
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            });
        }
        Setmeal setmeal = Setmeal.builder()
            .id(id)
            .status(status)
            .updateTime(LocalDateTime.now())
            .updateUser(BaseContext.getCurrentId())
            .build();
        setmealMapper.update(setmeal);
    }
    /**
     * 根据id查询套餐和关联的菜品数据
     * @param id
     * @return
    */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //先查询数据库,查到的原先的setmeal
        Setmeal setmeal = setmealMapper.getById(id);

        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }
    /**
     * 修改套餐
     */
    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //1.修改套餐表,执行update
        setmealMapper.update(setmeal);
        //2.删除套餐菜品表里的数据即操作setmeal_dish表,执行delete
        Long setmealId = setmeal.getId();
        setmealDishMapper.deleteBySetmealId(setmealId);
        //3.重新插入新的套餐菜品数据即操作setmeal_dish表,执行insert
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId((setmealId));
        });
        setmealDishMapper.insertBatch(setmealDishes);       
    }
    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }
    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
