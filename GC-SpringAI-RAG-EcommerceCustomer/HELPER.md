http://localhost:8080/ecommerce/service/chat/precise?question=新疆地区订单多少金额包邮?
http://localhost:8080/ecommerce/service/chat/enhanced?question=双11买的口红拆封了能退吗？我是VIP用户？
http://localhost:8080/ecommerce/service/chat/enhanced?question=我刚下单还没发货，想修改收货地址可以吗？
http://localhost:8080/ecommerce/service/chat/precise?question=怎么办理信用卡？


## 部署
- docker-compose.yml
```yml
version: '3.5'

services:
  etcd:
    container_name: milvus-etcd
    image: quay.io/coreos/etcd:v3.5.5
    environment:
      - ETCD_AUTO_COMPACTION_MODE=revision
      - ETCD_AUTO_COMPACTION_RETENTION=1000
    command: >
      etcd
      --advertise-client-urls=http://etcd:2379
      --listen-client-urls=http://0.0.0.0:2379
    ports:
      - "2379:2379"
    volumes:
      - ./volumes/etcd:/etcd

  minio:
    container_name: milvus-minio
    image: minio/minio:RELEASE.2023-03-20T20-16-18Z
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data
    ports:
      - "9000:9000"
    volumes:
      - ./volumes/minio:/data

  milvus:
    container_name: milvus-standalone
    image: milvusdb/milvus:v2.3.9
    command: ["milvus", "run", "standalone"]
    environment:
      ETCD_ENDPOINTS: etcd:2379
      MINIO_ADDRESS: minio:9000
    ports:
      - "19530:19530"
      - "9091:9091"
    volumes:
      - ./volumes/milvus:/var/lib/milvus
    depends_on:
      - etcd
      - minio
```


```bash
# 在与docker-compose.yml同级目录执行部署
docker compose up -d
# 查看是否成功 
docker ps
# 应该看到：
# milvus-etcd
# milvus-minio
# milvus-standalone

# 连接测试
nc -zv localhost 19530



```


