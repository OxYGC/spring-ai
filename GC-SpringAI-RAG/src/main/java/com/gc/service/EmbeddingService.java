package com.gc.service;

import com.gc.common.SimilarityCalculator;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;
    // 本地知识库文本（旅行相关场景）
    private final List<String> docs = List.of(
            "海边露营需准备防水帐篷、防潮垫、速干衣和便携炊具。",
            "山地徒步要携带登山杖、防滑鞋、双肩包和应急医疗包。",
            "城市漫游推荐骑行共享单车，打卡老街区和小众咖啡馆。"
    );
    // 知识库文本对应的向量（项目启动时初始化）
    private final List<float[]> docVectors;

    // 算法类型枚举（方便调用）
    public enum SimilarityAlgorithm {
        COSINE,        // 余弦相似度（默认）
        EUCLIDEAN,     // 欧氏距离
        MANHATTAN ,   // 曼哈顿距离
        PEARSON ,   // 曼哈顿距离
        MAHALANOBIS
    }

    // 构造方法注入 EmbeddingModel，初始化知识库向量
    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.docVectors = this.embeddingModel.embed(docs);
    }

    /**
     * 查找与查询文本最相似的知识库文本（指定算法）
     * @param query 用户查询文本
     * @param algorithm 相似度算法
     * @return 最相似的文本
     */
    public String queryBestMatch(String query, SimilarityAlgorithm algorithm) {
        float[] queryVec = embeddingModel.embed(query);
        int bestIdx = -1;
        double bestSim = -1;

        for (int i = 0; i < docVectors.size(); i++) {
            double sim = calculateSimilarity(queryVec, docVectors.get(i), algorithm);
            if (sim > bestSim) {
                bestSim = sim;
                bestIdx = i;
            }
        }
        return docs.get(bestIdx);
    }

    /**
     * 重载：默认使用余弦相似度
     */
    public String queryBestMatch(String query) {
        return queryBestMatch(query, SimilarityAlgorithm.COSINE);
    }

    /**
     * 统一相似度计算入口
     */
    private double calculateSimilarity(float[] a, float[] b, SimilarityAlgorithm algorithm) {
        return switch (algorithm) {
            case COSINE -> SimilarityCalculator.cosineSimilarity(a, b);
            case EUCLIDEAN -> SimilarityCalculator.euclideanSimilarity(a, b);
            case MANHATTAN -> SimilarityCalculator.manhattanSimilarity(a, b);
            case PEARSON -> SimilarityCalculator.pearsonCorrelation(a, b);
            case MAHALANOBIS -> {
                // 简化示例：使用单位矩阵作为协方差矩阵（实际需根据数据计算）
                double[][] covMatrix = new double[a.length][a.length];
                for (int i = 0; i < a.length; i++) {
                    covMatrix[i][i] = 1.0;
                }
                yield SimilarityCalculator.mahalanobisSimilarity(a, b, covMatrix);
            }
        };
    }
}