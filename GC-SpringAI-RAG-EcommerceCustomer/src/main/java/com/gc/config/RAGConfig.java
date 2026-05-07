package com.gc.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RAGConfig {
    private final VectorStore vectorStore;

    public RAGConfig(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 配置 QuestionAnswerAdvisor（适合精准条款查询，如具体政策匹配）
     */
    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor() {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.07)  // 优化阈值，适配电商规则检索
                        .topK(4)                   // 取 Top4 相似片段，提升精准度
                        .build())
                .build();
    }

    /**
     * 配置 RetrievalAugmentationAdvisor（适合复杂场景查询，如促销+退换货组合问题）
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor() {
        // 1. 向量检索器（配置检索参数）
        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.07)
                .topK(4)
                .build();

        // 查询增强器
        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
		                        .allowEmptyContext(true)
                                .build();
        // 3. 构建检索增强 Advisor
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }

    /**
     * 配置 ChatClient（集成智普 AI GLM-4-Flash，适配电商客服语气）
     */
    @Bean
    public ChatClient chatClient(org.springframework.ai.chat.model.ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是友好的电商客服顾问，仅基于提供的知识库内容回答用户问题，规则如下：
                        1. 回答需亲切、简洁、准确，符合电商客服沟通语气，避免生硬表述；
                        2. 涉及政策规则时，分点说明关键信息，让用户一目了然；
                        3. 回答末尾必须标注信息来源（格式：信息来源：[文档名称 - 相关条款类别]）；
                        4. 若未查询到相关信息，回复"非常抱歉，暂未查询到该问题的相关规则，建议联系人工客服咨询~"；
                        5. 仅回应与电商购物（退换货、促销、物流等）相关的问题，无关问题直接回复上述统一话术。
                        """)
                .build();
    }
}