package com.gc.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeBaseConfig {

    private final VectorStore vectorStore;
    public KnowledgeBaseConfig(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 项目启动时初始化知识库：解析 PDF 文档 → 切分文本 → 向量入库
     */
    @PostConstruct
    public void initKnowledgeBase() {
        try {
            System.out.println("开始初始化电商客服知识库...");
            // 1. 定义需要导入的 PDF 文档列表
            List<String> pdfFiles = List.of(
                    "电商知识库标准条款.docx"
            );

            List<Document> allSplitDocs = new ArrayList<>();
            for (String fileName : pdfFiles) {
                // 2. 读取单个 PDF 文档
                Resource resource = new ClassPathResource(fileName);
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                List<Document> rawDocs = reader.read();

                // 3. 优化文本切分策略（适配电商规则条款化特点）
                TokenTextSplitter splitter = TokenTextSplitter.builder()
                        .withChunkSize(600)          // 每段最大 600 Token（匹配规则条款长度）
                        .withMinChunkSizeChars(200)  // 每段最小 200 字符
                        .withKeepSeparator(true)     // 保留条款分隔符
                        .build();

                // 4. 切分当前文档并添加到总列表
                List<Document> splitDocs = splitter.apply(rawDocs);
                allSplitDocs.addAll(splitDocs);
                System.out.println("已解析文档：" + fileName + "，生成 " + splitDocs.size() + " 个文本片段");
            }

            // 5. 批量向量入库（Spring AI 自动调用 EmbeddingModel 完成向量化）
            vectorStore.add(allSplitDocs);
            System.out.println("知识库初始化完成，共导入 " + allSplitDocs.size() + " 个文本片段");
        } catch (Exception e) {
            System.err.println("知识库初始化失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}