package com.gc.gcspringaiembedding.controller;

import com.gc.gcspringaiembedding.service.EmbeddingService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class EmbeddingController {
    // 自动注入 EmbeddingModel（智普 AI 实现）
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private EmbeddingService embeddingService;
    /**
     * 文本向量化接口
     * @param message 待向量化的文本（默认值：推荐一款入门级露营装备）
     * @return 包含原始文本与向量的响应
     */
    @GetMapping("/embedding")
    public Map<String, Object> embedding(
            @RequestParam(value = "message", defaultValue = "推荐一款入门级露营装备") String message) {
        // 调用 embed 方法完成文本向量化（默认返回 1024 维向量）
        float[] vector = embeddingModel.embed(message);
        // 返回结果（原始文本 + 向量）
        return Map.of(
                "message", message,
                "vectorDimension", vector.length, // 向量维度
                "vector", vector
        );
    }

    /**
     * 相似文本查找接口（支持指定算法）
     * @param query 查询文本
     * @param algorithm 相似度算法（默认 COSINE）
     */
    @GetMapping("/similarity")
    public Map<String, Object> findSimilarText(
            @RequestParam("query") String query,
            @RequestParam(value = "algorithm", defaultValue = "COSINE") String algorithm) {
        // 校验算法参数合法性
        EmbeddingService.SimilarityAlgorithm simAlgo;
        try {
            simAlgo = EmbeddingService.SimilarityAlgorithm.valueOf(algorithm.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Map.of(
                    "error", "算法参数无效，支持的算法：COSINE/EUCLIDEAN/MANHATTAN/PEARSON/JACCARD/ADJUSTED_COSINE/MAHALANOBIS",
                    "code", 400
            );
        }
        // 执行相似文本查找
        String similarText = embeddingService.queryBestMatch(query, simAlgo);
        return Map.of(
                "query", query,
                "algorithm", simAlgo.name(),
                "answer", similarText
        );
    }




}