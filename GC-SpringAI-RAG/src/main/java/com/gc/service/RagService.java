package com.gc.service;

import com.gc.common.SimilarityCalculator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {
    // Embedding 模型（文本向量化）
    private final EmbeddingModel embeddingModel;
    // Chat 模型（生成回答）
    private final ChatClient chatClient;
    // 本地知识库文本片段
    private final List<String> docChunks = new ArrayList<>();
    // 文本片段对应的向量
    private final List<float[]> docVectors = new ArrayList<>();
    // 相似度算法（默认余弦相似度，可根据需求修改）
    private final EmbeddingService.SimilarityAlgorithm similarityAlgorithm = EmbeddingService.SimilarityAlgorithm.COSINE;
    // 构造方法注入依赖，初始化知识库（索引阶段）
    public RagService(EmbeddingModel embeddingModel, ChatClient.Builder chatClientBuilder) throws IOException {
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClientBuilder.build();
        // 加载本地知识库文件并切分片段
        loadAndSplitDocument();
    }
    /**
     * 加载本地知识库文件，切分为文本片段（索引阶段第一步）
     */
    private void loadAndSplitDocument() throws IOException {
        // 读取 resources 目录下的知识库文件
        Resource resource = new ClassPathResource("户外旅行安全指南.txt");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        // 按 "----" 切分文本（根据文件格式自定义切分规则）
        String[] chunks = content.split("----");
        for (String chunk : chunks) {
            String cleanChunk = chunk.strip();
            if (!cleanChunk.isBlank()) {
                docChunks.add(cleanChunk);
                // 文本片段向量化并缓存（索引阶段第二步、第三步）
                docVectors.add(embeddingModel.embed(cleanChunk));
            }
        }
    }
    /**
     * 处理用户提问，返回 RAG 增强后的回答（检索+生成阶段）
     * @param question 用户问题
     * @return 大模型生成的回答
     */
    public String answer(String question) {
        // 1. 用户问题向量化（检索阶段第一步）
        float[] questionVector = embeddingModel.embed(question);
        // 2. 检索 Top2 最相似的文本片段（检索阶段第二步、第三步）
        List<String> topRelevantChunks = retrieveTopRelevantChunks(questionVector, 2);
        // 3. 构建上下文（生成阶段第一步）
        String context = String.join("\n---\n", topRelevantChunks);
        // 4. 构建提示词（生成阶段第二步）
        String prompt = String.format(
                "以下是户外旅行安全指南的知识：\n%s\n请基于上述知识，简洁明了地回答问题：%s",
                context, question
        );
        // 5. 调用 Chat 模型生成回答（生成阶段第三步）
        return chatClient.prompt()
                .system("你是户外旅行安全助手，仅基于提供的上下文回答问题，不添加额外信息。")
                .user(prompt)
                .call()
                .content();
    }
    /**
     * 检索 TopK 最相似的文本片段（检索阶段核心逻辑）
     * @param questionVector 用户问题向量
     * @param topK 返回前 K 个相似片段
     * @return TopK 相似文本片段
     */
    private List<String> retrieveTopRelevantChunks(float[] questionVector, int topK) {
        List<ChunkSimilarity> similarityList = new ArrayList<>();
        // 计算问题向量与所有文本片段向量的相似度（使用指定算法）
        for (int i = 0; i < docVectors.size(); i++) {
            double sim = calculateSimilarity(questionVector, docVectors.get(i));
            similarityList.add(new ChunkSimilarity(i, sim));
        }
        // 按相似度降序排序，取前 topK 个
        similarityList.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        return similarityList.stream()
                .limit(topK)
                .map(item -> docChunks.get(item.index))
                .toList();
    }
    /**
     * 相似度计算（调用工具类，支持算法切换）
     */
    private double calculateSimilarity(float[] a, float[] b) {
        return switch (similarityAlgorithm) {
            case COSINE -> SimilarityCalculator.cosineSimilarity(a, b);
            case EUCLIDEAN -> SimilarityCalculator.euclideanSimilarity(a, b);
            case PEARSON -> SimilarityCalculator.pearsonCorrelation(a, b);
            case MANHATTAN -> SimilarityCalculator.manhattanSimilarity(a, b);
            default -> SimilarityCalculator.cosineSimilarity(a, b); // 默认 fallback 到余弦相似度
        };
    }
    /**
     * 辅助类：存储文本片段索引与相似度
     */
    private static class ChunkSimilarity {
        int index;
        double similarity;
        ChunkSimilarity(int index, double similarity) {
            this.index = index;
            this.similarity = similarity;
        }
    }
}