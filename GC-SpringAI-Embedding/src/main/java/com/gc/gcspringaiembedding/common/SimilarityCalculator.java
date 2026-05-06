package com.gc.gcspringaiembedding.common;

/**
 * 基于 Embedding 向量的文本相似度计算工具类
 * 整合多种核心相似度算法
 */
public class SimilarityCalculator {

    // ====================== 1. 余弦相似度（默认推荐） ======================
    public static double cosineSimilarity(float[] a, float[] b) {
        checkVectorLength(a, b);
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += Math.pow(a[i], 2);
            normB += Math.pow(b[i], 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ====================== 2. 欧氏距离（归一化后） ======================
    public static double euclideanSimilarity(float[] a, float[] b) {
        checkVectorLength(a, b);
        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }

        double distance = Math.sqrt(sum);
        // 归一化：1/(1+距离)，将距离转换为相似度（值越大相似度越高）
        return 1.0 / (1.0 + distance);
    }

    // ====================== 3. 曼哈顿距离（归一化后） ======================
    public static double manhattanSimilarity(float[] a, float[] b) {
        checkVectorLength(a, b);
        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }

        // 归一化：1/(1+距离)
        return 1.0 / (1.0 + sum);
    }

    // ====================== 4. 马氏相似度（归一化后） ======================
    public static double mahalanobisSimilarity(float[] a, float[] b, double[][] covarianceMatrix) {
        checkVectorLength(a, b);
        checkMatrix(covarianceMatrix, a.length);

        double[][] inverseCovariance = invertMatrix(covarianceMatrix);
        double[] delta = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            delta[i] = a[i] - b[i];
        }

        double quadraticForm = 0.0;
        for (int i = 0; i < delta.length; i++) {
            for (int j = 0; j < delta.length; j++) {
                quadraticForm += delta[i] * inverseCovariance[i][j] * delta[j];
            }
        }

        double distance = Math.sqrt(Math.max(quadraticForm, 0.0));
        // 归一化：1/(1+距离)，将马氏距离转换为相似度（值越大相似度越高）
        return 1.0 / (1.0 + distance);
    }

    // ====================== 辅助方法 ======================
    /** 校验两个向量长度一致 */
    private static void checkVectorLength(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("向量长度不一致，无法计算相似度");
        }
    }

    /** 校验协方差矩阵维度 */
    private static void checkMatrix(double[][] matrix, int expectedSize) {
        if (matrix == null || matrix.length != expectedSize) {
            throw new IllegalArgumentException("协方差矩阵维度不正确，无法计算马氏相似度");
        }
        for (double[] row : matrix) {
            if (row == null || row.length != expectedSize) {
                throw new IllegalArgumentException("协方差矩阵必须为方阵");
            }
        }
    }

    /** 计算向量均值 */
    private static double getVectorMean(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            sum += v;
        }
        return sum / vector.length;
    }

    /** 使用高斯-约旦消元法求逆矩阵 */
    private static double[][] invertMatrix(double[][] matrix) {
        int n = matrix.length;
        double[][] augmented = new double[n][n * 2];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented[i][j] = matrix[i][j];
            }
            augmented[i][i + n] = 1.0;
        }

        for (int i = 0; i < n; i++) {
            int pivotRow = i;
            for (int row = i + 1; row < n; row++) {
                if (Math.abs(augmented[row][i]) > Math.abs(augmented[pivotRow][i])) {
                    pivotRow = row;
                }
            }

            if (Math.abs(augmented[pivotRow][i]) < 1e-12) {
                throw new IllegalArgumentException("协方差矩阵不可逆，无法计算马氏相似度");
            }

            if (pivotRow != i) {
                double[] temp = augmented[i];
                augmented[i] = augmented[pivotRow];
                augmented[pivotRow] = temp;
            }

            double pivot = augmented[i][i];
            for (int j = 0; j < n * 2; j++) {
                augmented[i][j] /= pivot;
            }

            for (int row = 0; row < n; row++) {
                if (row == i) {
                    continue;
                }
                double factor = augmented[row][i];
                for (int j = 0; j < n * 2; j++) {
                    augmented[row][j] -= factor * augmented[i][j];
                }
            }
        }

        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmented[i], n, inv[i], 0, n);
        }
        return inv;
    }
}
