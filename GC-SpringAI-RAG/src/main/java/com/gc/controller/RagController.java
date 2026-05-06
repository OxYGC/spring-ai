package com.gc.controller;

import com.gc.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
@RestController
@RequestMapping("/rag")
public class RagController {
    @Autowired
    private RagService ragService;
    /**
     * RAG 本地知识库检索接口
     * @param question 用户问题
     * @return 包含问题与回答的响应
     */
    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam("question") String question) {
        String answer = ragService.answer(question);
        return Map.of(
                "question", question,
                "answer", answer
        );
    }
}