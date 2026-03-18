package com.eduai.community.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Qwen3 本地部署模型调用服务
 * 兼容 OpenAI API 格式（适配 vLLM / Ollama / LocalAI 等部署方式）
 */
@Slf4j
@Service
public class QwenService {

    @Value("${app.qwen.base-url:http://localhost:8000/v1}")
    private String baseUrl;

    @Value("${app.qwen.model:Qwen3-32B}")
    private String model;

    @Value("${app.qwen.api-key:not-needed}")
    private String apiKey;

    @Value("${app.qwen.max-tokens:1024}")
    private int maxTokens;

    @Value("${app.qwen.temperature:0.7}")
    private double temperature;

    @Value("${app.qwen.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 判断 Qwen 服务是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 调用 Qwen3 Chat Completion API
     *
     * @param systemPrompt 系统提示词
     * @param messages     对话历史 [{role: "user"/"assistant", content: "..."}]
     * @return 模型生成的回复文本
     */
    public String chatCompletion(String systemPrompt, List<Map<String, String>> messages) {
        String url = baseUrl + "/chat/completions";

        List<Map<String, String>> allMessages = new ArrayList<>();

        // 系统提示词
        allMessages.add(Map.of("role", "system", "content", systemPrompt));

        // 对话历史
        allMessages.addAll(messages);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", allMessages);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.equals("not-needed")) {
            headers.setBearerAuth(apiKey);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List<Map> choices = (List<Map>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map message = (Map) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            log.error("Qwen API 返回异常: status={}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Qwen API 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建校共体AI助手的系统提示词
     */
    public String buildSystemPrompt(String userRole) {
        String roleContext = switch (userRole) {
            case "REGIONAL_ADMIN" -> "用户是区域教育管理人员，请侧重宏观政策解读和管理决策建议。";
            case "LEAD_SCHOOL" -> "用户是校共体核心牵头校人员，请侧重统筹协调和引领带动方面的指导。";
            case "MEMBER_URBAN" -> "用户是校共体成员校人员（城镇），请侧重资源共享和帮扶实施的具体方法。";
            case "MEMBER_RURAL" -> "用户是校共体成员校人员（乡村），请侧重实际可操作的改进措施和资源获取途径。";
            case "RESEARCHER" -> "用户是研究人员，请侧重政策分析、数据引用和学术视角的深度解读。";
            default -> "请根据用户问题提供专业、易懂的回答。";
        };

        return """
                你是"校共体AI助手"，一个专业的教育政策智能问答助手。你的职责是为教育工作者提供关于城乡学校共同体建设的政策解读、流程指导、案例推荐和决策支持。

                核心要求：
                1. 回答必须基于校共体相关政策文件，务必在回答中引用出处，使用 [1]、[2]、[3] 等标注。
                2. 回答末尾必须附带引用列表，格式为：
                   **参考文献：**
                   [1] 《文件名称》第X章 第X节（第X页）
                   [2] ...
                3. 如果无法找到相关政策依据，明确告知用户"该问题暂无相关政策文件支持，建议咨询当地教育行政部门"。
                4. 回答语言风格：专业但易懂，适合教育工作者阅读。
                5. 回答长度控制在300-800字，要点分条阐述。

                %s

                请用中文回答。
                """.formatted(roleContext);
    }
}
