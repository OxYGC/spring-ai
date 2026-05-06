# Spring AI 高级：RAG 优化与向量数据库集成实战

### 一、优化方向与核心策略

之前，我们归纳了基于 RAG 技术落地实践中存在的核心痛点。在正式开展优化工作之前，需先围绕这些痛点明确对应的优化方向与实施策略，以保障优化方案的精准性与有效性。

代码语言：javascript

AI代码解释

复制

```
使用 Spring AI 提供的 TokenTextSplitter，按 Token 数量切分（每段 800 Token，最小 400 字符），保留分隔符
```

### 二、优化准备：Milvus 向量数据库部署与集成

#### 1\. 什么是 Milvus？

Milvus 是一款开源的高性能向量[数据库](https://cloud.tencent.com/product/tencentdb-catalog?from_column=20065&from=20065)，专为 AI 场景设计，支持海量向量数据的存储、检索与管理。它具备以下核心优势：

*   支持多种距离计算（余弦相似度、欧氏距离等），适配 Embedding 向量检索场景。
*   高吞吐量与低延迟，可满足大规模知识库的快速检索需求。
*   兼容 Spring AI 生态，通过 Spring AI Starter 可快速集成。

#### 2\. Milvus 部署（Docker 方式）

##### **步骤 1：安装 Docker 与 Docker Compose**

确保本地已安装 Docker（版本 20.10+）与 Docker Compose（版本 2.10+），安装教程参考 Docker 官方文档。

##### **步骤 2：下载 Milvus 配置文件**

代码语言：javascript

AI代码解释

复制

```bash
# 创建 Milvus 目录
mkdir -p /software/milvus && cd /software/milvus
# 下载 docker-compose.yml 文件
wget https://github.com/milvus-io/milvus/releases/download/v2.4.0/milvus-standalone-docker-compose.yml -O docker-compose.yml
```

##### **步骤 3：启动 Milvus**

代码语言：javascript

AI代码解释

复制

```bash
# 后台启动 Milvus
docker compose up -d
# 查看启动状态（确保所有容器都为 running 状态）
docker compose ps
```

启动成功后，Milvus 服务默认监听端口 `19530`（gRPC）与 `9091`（HTTP）。

Milvus提供了后台管理服务，默认地址：http://10.8.0.233:8000/#

![](https://developer.qcloudimg.com/http-save/yehe-2935166/92111f62946db2efe891e2b703664c3e.png)

#### 3\. Spring AI 集成 Milvus 依赖配置

在 Spring Boot 项目（可以复制之前的Weiz-SpringAI-RAG项目）的 `pom.xml` 中添加 Milvus 依赖：

代码语言：javascript

AI代码解释

复制

```xml
<!-- Milvus 向量数据库 Starter -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-milvus</artifactId>
</dependency>
```

#### 4\. 配置 Milvus 连接信息

在 `src/main/resources/application.properties` 中添加 Milvus 配置：


```lombok.config
spring.application.name=Weiz-SpringAI-RAG-Milvus

server.port=8080

#AI Embedding
spring.ai.zhipuai.api-key=你的智谱api key
spring.ai.zhipuai.base-url=https://open.bigmodel.cn/api/paas
spring.ai.zhipuai.embedding.options.model=embedding-2

# AI Chat
spring.ai.zhipuai.chat.options.model=GLM-4-Flash

# Milvus 连接配置
spring.ai.vectorstore.milvus.client.host=10.8.0.233
spring.ai.vectorstore.milvus.client.port=19530
# 认证信息
spring.ai.vectorstore.milvus.client.username=root
spring.ai.vectorstore.milvus.client.password=hadoop
# 数据库名称（默认 default）
spring.ai.vectorstore.milvus.database-name=default
# 向量集合名称（自定义，不存在会自动创建）
spring.ai.vectorstore.milvus.collection-name=travel_safety_embedding
# 是否自动初始化 Schema（建议开启）
spring.ai.vectorstore.milvus.initialize-schema=true
# 向量维度（与智普 AI embedding-2 模型一致，为 1024）
spring.ai.vectorstore.milvus.embedding-dimension=1024
```

### 三、RAG 优化实战：生产级本地知识库检索

#### 1\. 编写优化后的 RAG Service

创建 `com.example.weizspringai.service.RagOptimizedService` 类，实现优化后的 RAG 逻辑：

代码语言：javascript

AI代码解释

复制

```java
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
```

#### 2\. 编写优化后的 RAG Controller

创建 com.example.weizspringai.controller.RagOptimizedController 类，提供优化后的 RAG 检索接口：

代码语言：javascript

AI代码解释

复制

```java
import com.example.weizspringai.service.RagOptimizedService;
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
```

#### 3\. 测试优化后的 RAG 系统

##### 测试 1：查询复杂问题（需要多片段拼接）

访问 http://localhost:8080/rag/optimized/ask?question=户外旅行中露营、徒步、城市漫游分别有哪些核心安全注意事项？，响应结果：

代码语言：javascript

AI代码解释

复制

```
{
  "question": "户外旅行中露营、徒步、城市漫游分别有哪些核心安全注意事项？",
  "answer": "户外旅行中三类场景的核心安全注意事项如下：\n1. 露营安全：选址避开低洼积水区、陡坡和落石区域，优先选平坦硬质地面；远离易燃物，使用炉具保持通风，睡前熄灭明火；携带驱蚊液和防虫网，避免在草丛密集处搭帐篷；随身携带手电筒、急救包和足量饮用水，提前下载离线地图。\n2. 徒步安全：提前查询路线难度和天气，选择适配自身体能的路线并告知亲友行程；穿防滑登山鞋、戴防晒帽，携带充足食物、水、登山杖和护膝；遇暴雨立即找高地躲避，不涉险过河，遭遇野生动物保持冷静不投喂；匀速前进，每小时休息10-15分钟，避免过度疲劳。\n3. 城市漫游安全：骑行遵守交通规则，不逆行闯红灯，佩戴安全头盔；乘坐公共交通妥善保管个人财物；避开偏僻小巷和治安较差区域，优先选择人流密集的商业街区和景点；保存当地派出所、医院联系方式，携带少量现金，保持手机电量充足。"
}
```

##### 测试 2：查询低相关问题（验证阈值过滤）

访问http://localhost:8080/rag/optimized/ask?question=室内健身房锻炼有哪些安全要点？，响应结果：

代码语言：javascript

AI代码解释

复制

```json
{
  "question": "室内健身房锻炼有哪些安全要点？",
  "answer": "抱歉，未查询到相关安全指南信息"
}
```

##### 测试 3：查询需要上下文扩展的问题

访问 http://localhost:8080/rag/optimized/ask?question=徒步时遇到野生动物该怎么处理？，响应结果：


```json
{
  "question": "徒步时遇到野生动物该怎么处理？",
  "answer": "徒步时遭遇野生动物，核心处理原则是保持冷静，切勿投喂或驱赶。同时需结合徒步安全的整体要求，避免单独行动，提前了解路线上的野生动物分布情况，遇到时与动物保持安全距离，缓慢撤离至安全区域，切勿惊慌奔跑引发动物追击。"
}
```

4\. 优化效果分析

测试结果表明，优化后的 RAG 系统具备以下生产级特性：

*   回答完整性：能整合多个相关片段信息，精准回应复杂问题，无语义断裂；
*   噪声过滤：通过相似度阈值有效过滤低相关内容，避免无关回答；
*   上下文连贯性：相邻片段扩展策略让回答逻辑更清晰，符合实际使用场景；
*   稳定性：向量存储基于 Milvus，服务重启后数据不丢失，支持长期使用。

### 五、生产环境注意事项

1.  **Milvus 集群部署**单机版 Milvus 仅适用于测试，生产环境需部署 Milvus 集群，确保高可用与高吞吐量。
2.  **文本切分参数调优**根据知识库类型调整 `chunkSize` 与 `minChunkSizeChars`，例如技术文档可设置为 1000 Token / 段，文学类文档可设置为 500 Token / 段。
3.  **相似度阈值动态调整**不同知识库的向量分布不同，需通过测试调整阈值（如 0.03~0.1），平衡召回率与精确率。
4.  **上下文长度限制**大模型有最大上下文长度限制（如 GLM-4-Flash 支持 8k Token），需控制拼接后的上下文长度，避免超限。
5.  **缓存优化**对高频查询的向量结果进行缓存（如使用 Redis），减少 Milvus 检索压力。

### 总结

本文通过集成 Milvus 向量数据库、优化文本切分策略、升级检索逻辑，成功解决了基础版 RAG 的核心痛点，打造了具备高可用性、高精准度的生产级 RAG 应用。核心优化价值在于：Milvus 提供了向量持久化存储能力，TokenTextSplitter 确保了文本片段的语义完整性，相似度阈值与相邻片段扩展提升了检索精度与上下文连贯性。

