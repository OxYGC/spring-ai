package com.gc.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class TripPlanningAgentService {
    private final ChatClient chatClient;

    public TripPlanningAgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Agent 核心能力：接收出行需求，生成完整行程规划
     *
     * @param tripDemand 用户出行需求（包含时间、地点、偏好等）
     * @return 完整行程规划（按天/按时段拆分）
     */
    public String planTrip(String tripDemand) {
        // 定义 Agent 行为规则：引导大模型完成行程规划
        String systemPrompt = """
                你是一个专业的智能行程规划 Agent，核心职责是根据用户出行需求，生成完整、可执行的行程方案，规则如下：
                1. 先拆解需求核心要素：出行时间、目的地、人数、偏好（景点类型、饮食、交通方式）、禁忌；
                2. 行程按天拆分，每天按时间段（上午/下午/晚上）规划，包含景点、交通、餐饮、停留时长；
                3. 景点选择贴合用户偏好，餐饮适配饮食禁忌，交通路线合理（避免绕路）；
                4. 补充实用提示（如景点开放时间、预约要求、穿搭建议）；
                5. 语言简洁明了，结构清晰，便于用户直接参考执行。
                """;

        try {
            // 调用大模型完成行程规划（模拟 Agent 自主规划能力）
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user("请根据以下出行需求生成完整行程规划：" + tripDemand)
                    .call()
                    .content();
            return content != null ? content : "AI模型未返回有效内容";
        } catch (Exception e) {
            System.out.println("调用AI模型失败" + e.getMessage());
            return "调用AI模型失败" + e.getMessage();
        }
    }
}
