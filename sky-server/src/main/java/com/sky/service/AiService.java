package com.sky.service;

/**
 * AI 服务接口
 */
public interface AiService {

    /**
     * 根据菜品名和分类，调用 DeepSeek 生成描述
     * @param dishName 菜品名
     * @param categoryName 分类名
     * @return AI 生成的描述文字
     */
    String generateDishDescription(String dishName, String categoryName);
}