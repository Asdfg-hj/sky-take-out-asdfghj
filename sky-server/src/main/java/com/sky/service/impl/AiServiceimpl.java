package com.sky.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class AiServiceimpl implements AiService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateDishDescription(String dishName, String categoryName, String flavorInfo) {
        // 1. 构建 Prompt
        String systemPrompt = "你是一个专业的美食文案写手，擅长用生动、诱人的语言描述菜品。";
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format("请为一道名为【%s】的%s菜品生成一段50字左右的诱人描述。", dishName, categoryName));
        if (flavorInfo != null && !flavorInfo.trim().isEmpty()) {
            promptBuilder.append(String.format("菜品属性：%s。", flavorInfo));
        }
        promptBuilder.append("要求：结合属性突出口味特点，让人看了就想吃。只返回描述内容，不要加其他废话。");
        String userPrompt = promptBuilder.toString();

        return callAiApi(dishName, systemPrompt, userPrompt);
    }

    @Override
    public String generateSetmealDescription(String setmealName, String categoryName, String dishListInfo) {
        // 1. 构建 Prompt
        String systemPrompt = "你是一个专业的美食文案写手，擅长用生动、诱人的语言描述套餐。";
        String userPrompt = String.format(
                "请为一份名为【%s】的套餐（分类：%s）生成一段50字左右的诱人描述。" +
                "套餐包含：%s。" +
                "要求：突出套餐的搭配优势和整体吸引力，让人看了就想下单。只返回描述内容，不要加其他废话。",
                setmealName, categoryName, dishListInfo
        );

        return callAiApi(setmealName, systemPrompt, userPrompt);
    }

    /**
     * 调用 DeepSeek API 的通用方法
     */
    private String callAiApi(String name, String systemPrompt, String userPrompt) {
        // 2. 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.8);
        requestBody.put("stream", false);

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        // 3. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 4. 发送请求
        try {
            log.info("调用 DeepSeek API，生成描述：{}", name);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String content = parseResponse(response.getBody());
                log.info("AI生成描述成功：{}", content);
                return content;
            } else {
                log.error("DeepSeek API 调用失败，状态码：{}", response.getStatusCode());
                return "（AI描述生成失败，请稍后重试）";
            }

        } catch (Exception e) {
            log.error("调用 DeepSeek API 异常", e);
            return "（AI描述生成失败，请稍后重试）";
        }
    }

    /**
     * 解析 DeepSeek 返回的 JSON
     */
    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText()
                .trim();
    }
}
