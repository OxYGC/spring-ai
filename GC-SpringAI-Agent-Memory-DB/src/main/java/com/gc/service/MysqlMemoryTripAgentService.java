package com.gc.service;

import  org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import  org.springframework.ai.chat.memory.ChatMemory;
import  org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import  org.springframework.stereotype.Service;
import  java.util.List;

@Service
public  class  MysqlMemoryTripAgentService {

    private final ChatClient chatClient;

    private final ChatMemory chatMemory;
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;
    // 注入两种记忆类型，支持动态切换
    private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;
    private final PromptChatMemoryAdvisor promptChatMemoryAdvisor;

    public MysqlMemoryTripAgentService(ChatClient chatClient,
                                       ChatMemory chatMemory,
                                       JdbcChatMemoryRepository jdbcChatMemoryRepository,
                                       MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                       PromptChatMemoryAdvisor promptChatMemoryAdvisor) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
        this.messageChatMemoryAdvisor = messageChatMemoryAdvisor;
        this.promptChatMemoryAdvisor = promptChatMemoryAdvisor;
    }

    /**
     * 生产级带 MySQL 记忆的行程规划
     *
     * @param userId     用户唯一标识（即 conversationId，隔离多用户）
     * @param demand     出行需求
     * @param memoryType 记忆类型：message/prompt
     * @return 个性化行程规划
     */
    public String planTripWithMysqlMemory(String userId, String demand, String memoryType) {
        // 动态选择记忆类型
        BaseChatMemoryAdvisor selectedAdvisor = "prompt".equals(memoryType)
                ? promptChatMemoryAdvisor
                : messageChatMemoryAdvisor;
        // 生产级 Agent 行为规则：强化记忆复用、输出准确性
        String systemPrompt = """
                你是生产级智能行程规划 Agent，严格遵守以下规则：
                1. 优先从 MySQL 存储的历史记忆中提取用户偏好（景点类型、饮食禁忌、交通方式、出行人数）；
                2. 无需用户重复说明已存储的偏好，新需求可覆盖旧记忆；
                3. 行程按天/时段拆分，包含景点、交通、餐饮、实用提示（开放时间、预约要求），信息准确可执行；
                4. 语言简洁专业，适配移动端阅读，避免冗余表述；
                5. 未查询到记忆时，按当前需求正常规划，不提示记忆相关信息。
                """;
        // 调用大模型：关联用户 MySQL 记忆
        return chatClient.prompt()
                .system(systemPrompt)
                .user(demand)
                .advisors(selectedAdvisor)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .content();
    }

    /**
     * 清除用户 MySQL 记忆（生产级必备功能）
     */
    public void clearUserMemory(String userId) {
        chatMemory.clear(userId);
    }

    /**
     * 查询所有用户对话 ID（管理端使用）
     */
    public List<String> listAllConversationIds() {
        // 注入 JdbcChatMemoryRepository 以查询所有对话 ID
        return jdbcChatMemoryRepository.findConversationIds();
    }
}
