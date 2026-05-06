package com.gc.service;

import com.gc.common.SimilarityCalculator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class RagOptimizedService {
    // 注入核心依赖
    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    // 缓存切分后的文本片段，避免重复读取
    private final List<String> docChunks = new ArrayList<>();

    // 构造方法注入依赖
    public RagOptimizedService(EmbeddingModel embeddingModel, ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    /**
     * 项目启动时初始化：加载知识库、切分文本、向量入库（仅执行一次）
     */
    @PostConstruct
    public void initKnowledgeBase() throws IOException {
        System.out.println("开始初始化知识库，加载文件并写入 Milvus 向量数据库...");
        // 1. 加载本地知识库文件（户外旅行安全指南）
        Resource resource = new ClassPathResource("户外旅行安全指南.txt");
        TextReader textReader = new TextReader(resource);
        List<Document> rawDocs = textReader.read();

        // 2. 优化文本切分（TokenTextSplitter 按语义切分）
        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(800)          // 每段最大 800 Token
                .withMinChunkSizeChars(400)  // 每段最小 400 字符（避免过短）
                .withKeepSeparator(true)     // 保留分隔符，提升语义连贯性
                .build();
        List<Document> splitDocs = textSplitter.apply(rawDocs);

        // 3. 缓存文本片段，同时将向量写入 Milvus
        for (Document doc : splitDocs) {
            String text = doc.getText().strip();
            if (!text.isBlank()) {
                docChunks.add(text);
                // 向量入库（Spring AI 自动调用 EmbeddingModel 完成向量化）
                vectorStore.add(List.of(doc));
            }
        }
        System.out.println("知识库初始化完成，共加载 " + docChunks.size() + " 个文本片段");
    }

    /**
     * 处理用户提问，返回优化后的 RAG 回答
     * @param question 用户问题
     * @return 大模型生成的回答
     */
    public String answer(String question) {
        // 1. 生成用户问题的向量
        float[] questionVector = embeddingModel.embed(question);
        // 2. 检索相关片段：Top5 + 相似度阈值 + 相邻片段扩展
        List<String> relevantChunks = retrieveRelevantChunks(questionVector, 5, 0.05);
        // 3. 构建上下文（拼接相关片段，去除重复内容）
        String context = relevantChunks.stream()
                .distinct()
                .collect(Collectors.joining("\n---\n"));
        // 4. 构建提示词（明确要求基于上下文回答，禁止添加额外信息）
        String prompt = String.format(
                "以下是户外旅行安全指南的知识库内容：\n%s\n请严格基于上述内容，简洁、准确地回答用户问题，不要添加额外信息。问题：%s",
                context, question
        );
        // 5. 调用 Chat 模型生成回答
        return chatClient.prompt()
                .system("你是专业的户外旅行安全助手，仅基于提供的上下文回答问题，若上下文无相关信息，回复'抱歉，未查询到相关安全指南信息'。")
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 检索相关文本片段：TopK + 相似度阈值 + 相邻片段扩展
     * @param questionVector 问题向量
     * @param topK 最多返回 K 个相似片段
     * @param threshold 相似度阈值（低于该值的片段过滤）
     * @return 最终用于构建上下文的片段列表
     */
    private List<String> retrieveRelevantChunks(float[] questionVector, int topK, double threshold) {
        // 存储需要保留的片段索引（TreeSet 自动去重并排序）
        Set<Integer> targetIndexes = new TreeSet<>();
        // 1. 计算问题向量与所有片段向量的相似度
        List<ChunkSimilarity> similarityList = new ArrayList<>();
        for (int i = 0; i < docChunks.size(); i++) {
            float[] chunkVector = embeddingModel.embed(docChunks.get(i));
            double sim = SimilarityCalculator.cosineSimilarity(questionVector, chunkVector);
            // 过滤低于阈值的低相关片段
            if (sim >= threshold) {
                similarityList.add(new ChunkSimilarity(i, sim));
            }
        }
        // 2. 按相似度降序排序，取 TopK 个高相关片段
        similarityList.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        List<ChunkSimilarity> topKInfos = similarityList.stream()
                .limit(topK)
                .collect(Collectors.toList());
        // 3. 扩展相邻片段（每个相关片段的前后各 1 个），提升上下文连贯性
        for (ChunkSimilarity info : topKInfos) {
            int index = info.index;
            targetIndexes.add(index); // 当前高相关片段
            if (index - 1 >= 0) {
                targetIndexes.add(index - 1); // 前一个相邻片段
            }
            if (index + 1 < docChunks.size()) {
                targetIndexes.add(index + 1); // 后一个相邻片段
            }
        }
        // 4. 转换为文本片段列表返回
        return targetIndexes.stream()
                .map(docChunks::get)
                .collect(Collectors.toList());
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