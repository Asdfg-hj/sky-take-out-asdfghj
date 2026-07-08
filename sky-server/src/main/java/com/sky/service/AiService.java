package com.sky.service;

/**
 * AI 服务接口
 */
public interface AiService {

    /**
     * 根据菜品名、分类和口味信息，调用 AI 生成描述
     * @param dishName 菜品名
     * @param categoryName 分类名
     * @param flavorInfo 口味信息，如 "辣度可选（不辣/微辣/重辣）、温度可选（热饮/常温）"
     * @return AI 生成的描述文字
     */
    String generateDishDescription(String dishName, String categoryName, String flavorInfo);

    /**
     * 根据套餐名、分类和包含的菜品列表，调用 AI 生成描述
     * @param setmealName 套餐名
     * @param categoryName 分类名
     * @param dishListInfo 套餐包含的菜品列表，如 "宫保鸡丁 x1、麻婆豆腐 x1、米饭 x2"
     * @return AI 生成的描述文字
     */
    String generateSetmealDescription(String setmealName, String categoryName, String dishListInfo);
}