package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.AiService;           // ← 注入接口
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
    private AiService aiService;            // ← 注入的是接口，不是实现类

    @PostMapping("/generateDescription")
    @ApiOperation("AI生成菜品描述")
    public Result<String> generateDescription(@RequestBody Map<String, String> params) {
        String dishName = params.get("dishName");
        String categoryName = params.get("categoryName");

        if (dishName == null || dishName.trim().isEmpty()) {
            return Result.error("菜品名不能为空");
        }

        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "中式";
        }

        log.info("AI生成描述：菜品={}, 分类={}", dishName, categoryName);
        String description = aiService.generateDishDescription(dishName, categoryName);
        return Result.success(description);
    }
}
