package com.gc.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/ecommerce/service")
public class CustomerServiceController {
    @Autowired
    private ChatClient chatClient;

    @Autowired
    private QuestionAnswerAdvisor questionAnswerAdvisor;

    @Autowired
    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    /**
     * 精准条款查询接口（基于 QuestionAnswerAdvisor）
     * 适用场景：查询具体规则条款（如包邮条件、退换货时限、价保规则）
     */
    @GetMapping("/chat/precise")
    public Map<String, String> preciseChat(@RequestParam("question") String question) {
        String answer = chatClient.prompt()
                .user(question)
                .advisors(List.of(questionAnswerAdvisor))
                .call()
                .content();
        return Map.of(
                "question", question,
                "answer", answer,
                "mode", "precise（精准条款查询）"
        );
    }

    /**
     * 复杂场景增强查询接口（基于 RetrievalAugmentationAdvisor）
     * 适用场景：组合类、流程类问题（如促销期退换货、已下单改地址、VIP用户物流查询）
     */
    @GetMapping("/chat/enhanced")
    public Map<String, String> enhancedChat(@RequestParam("question") String question) {
        String answer = chatClient.prompt()
                .user(question)
                .advisors(List.of(retrievalAugmentationAdvisor))
                .call()
                .content();
        return Map.of(
                "question", question,
                "answer", answer,
                "mode", "enhanced（复杂场景增强）"
        );
    }
}