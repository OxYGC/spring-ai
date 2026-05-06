package com.gc.controller;

import com.gc.service.RagOptimizedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/rag/optimized")
public class RagOptimizedController {

    @Autowired
    private RagOptimizedService ragOptimizedService;

    /**
     * 优化后的 RAG 本地知识库检索接口
     * @param question 用户问题
     * @return 包含问题与回答的响应
     */
    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam("question") String question) {
        String answer = ragOptimizedService.answer(question);
        return Map.of(
                "question", question,
                "answer", answer
        );
    }
}