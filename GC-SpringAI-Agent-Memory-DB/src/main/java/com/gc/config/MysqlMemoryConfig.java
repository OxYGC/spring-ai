package com.gc.config;

import org.springframework.ai.chat.client.ChatClient;
import  org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import  org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import  org.springframework.ai.chat.memory.ChatMemory;
import  org.springframework.ai.chat.memory.MessageWindowChatMemory;
import  org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import  org.springframework.context.annotation.Bean;
import  org.springframework.context.annotation.Configuration;

@Configuration
public  class  MysqlMemoryConfig {
    // 注入 Spring AI 内置的 JdbcChatMemoryRepository（自动适配 MySQL）
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    public MysqlMemoryConfig(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
    }

    /**
     * Spring AI 2.x 只会自动提供 ChatClient.Builder，
     * 这里显式构建 ChatClient Bean，供业务层直接注入使用。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * 配置 ChatMemory：基于 MySQL 存储+消息窗口限制
     * 最大记忆 30 条，避免表数据膨胀
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)  // 绑定 MySQL 存储
                .maxMessages(30)  // 限制最大记忆条数
                .build();
    }

    /**
     * 记忆类型 1：MessageChatMemoryAdvisor（角色对话模式）
     * 适用于支持角色对话的大模型（如 GLM-4、GPT 系列）
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }

    /**
     * 记忆类型 2：PromptChatMemoryAdvisor（Prompt 封装模式）
     * 适用于不支持角色对话的大模型（记忆封装到 System Prompt）
     */
    @Bean
    public PromptChatMemoryAdvisor promptChatMemoryAdvisor(ChatMemory chatMemory) {
        return PromptChatMemoryAdvisor.builder(chatMemory)
                .build();
    }
}
