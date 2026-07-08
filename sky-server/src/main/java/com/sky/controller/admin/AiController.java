package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.AiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/ai")
@Api(tags = "AI助手接口")
@Slf4j
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/generateDescription")
    @ApiOperation("AI生成菜品描述")
    public Result<String> generateDescription(@RequestBody Map<String, String> params) {
        String dishName = params.get("dishName");
        String categoryName = params.get("categoryName");
        String flavorInfo = params.getOrDefault("flavorInfo", "");

        if (dishName == null || dishName.trim().isEmpty()) {
            return Result.error("菜品名不能为空");
        }

        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "中式";
        }

        log.info("AI生成描述：菜品={}, 分类={}, 口味={}", dishName, categoryName, flavorInfo);
        String description = aiService.generateDishDescription(dishName, categoryName, flavorInfo);
        return Result.success(description);
    }

    @PostMapping("/generateSetmealDescription")
    @ApiOperation("AI生成套餐描述")
    public Result<String> generateSetmealDescription(@RequestBody Map<String, String> params) {
        String setmealName = params.get("setmealName");
        String categoryName = params.get("categoryName");
        String dishListInfo = params.getOrDefault("dishListInfo", "");

        if (setmealName == null || setmealName.trim().isEmpty()) {
            return Result.error("套餐名不能为空");
        }

        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "中式";
        }

        log.info("AI生成套餐描述：套餐={}, 分类={}, 菜品={}", setmealName, categoryName, dishListInfo);
        String description = aiService.generateSetmealDescription(setmealName, categoryName, dishListInfo);
        return Result.success(description);
    }
}
