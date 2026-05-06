package com.gc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimilarityCalculatorTest {

    @Test
    void pearsonCorrelationReturnsOneForPerfectPositiveCorrelation() {
        float[] a = {1.0f, 2.0f, 3.0f, 4.0f};
        float[] b = {2.0f, 4.0f, 6.0f, 8.0f};

        assertEquals(1.0, SimilarityCalculator.pearsonCorrelation(a, b), 1e-9);
    }

    @Test
    void pearsonCorrelationReturnsMinusOneForPerfectNegativeCorrelation() {
        float[] a = {1.0f, 2.0f, 3.0f, 4.0f};
        float[] b = {8.0f, 6.0f, 4.0f, 2.0f};

        assertEquals(-1.0, SimilarityCalculator.pearsonCorrelation(a, b), 1e-9);
    }

    @Test
    void pearsonCorrelationReturnsZeroWhenVarianceIsZero() {
        float[] a = {1.0f, 1.0f, 1.0f};
        float[] b = {2.0f, 3.0f, 4.0f};

        assertEquals(0.0, SimilarityCalculator.pearsonCorrelation(a, b), 1e-9);
    }
}
