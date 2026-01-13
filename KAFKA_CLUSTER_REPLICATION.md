# Kafka 클러스터 및 레플리케이션 구성

## 개요
고가용성(High Availability)과 내결함성(Fault Tolerance)을 위해 3-broker Kafka 클러스터와 3-way 레플리케이션을 적용했습니다.

## 아키텍처

### Kafka 브로커 클러스터 구성
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Kafka-1    │     │  Kafka-2    │     │  Kafka-3    │
│  Broker ID:1│     │  Broker ID:2│     │  Broker ID:3│
│  Port: 9092 │     │  Port: 9093 │     │  Port: 9094 │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │
       └───────────────────┴───────────────────┘
                           │
                    ┌──────┴──────┐
                    │  Zookeeper  │
                    │  Port: 2181 │
                    └─────────────┘
```

### 레플리케이션 구조
```
Topic: coupon-events (Partition 0)
┌──────────────────────────────────────────┐
│ Leader Replica (Kafka-1)                 │ ◄── Producer writes here
├──────────────────────────────────────────┤
│ Follower Replica (Kafka-2)               │ ◄── Sync from leader
├──────────────────────────────────────────┤
│ Follower Replica (Kafka-3)               │ ◄── Sync from leader
└──────────────────────────────────────────┘

ISR (In-Sync Replicas): 최소 2개 이상 유지
```

## 주요 설정

### 1. Replication Factor: 3
- 모든 토픽의 데이터가 3개 브로커에 복제됨
- 최대 2개 브로커 장애까지 데이터 보존 가능

### 2. Min In-Sync Replicas (Min ISR): 2
- Producer가 메시지 전송 시 최소 2개 브로커의 확인 필요
- Leader + 최소 1개 Follower가 sync 상태여야 함
- 데이터 일관성 보장

### 3. Producer ACKs: all
- 모든 ISR 브로커의 확인을 기다림
- 데이터 손실 방지 (가장 안전한 설정)

### 4. Idempotence: true
- 중복 메시지 방지
- Exactly-Once Semantics 보장

## 토픽별 구성

| 토픽 | 파티션 | 레플리카 | Min ISR | Retention |
|------|--------|----------|---------|-----------|
| coupon-events | 3 | 3 | 2 | 7일 |
| coupon-events-dlq | 1 | 3 | 2 | 30일 |
| order-events | 3 | 3 | 2 | 7일 |
| order-events-dlq | 1 | 3 | 2 | 30일 |
| stock-events | 3 | 3 | 2 | 7일 |
| stock-events-dlq | 1 | 3 | 2 | 30일 |

## 장애 시나리오별 동작

### 시나리오 1: 1개 브로커 장애
```
Before:
Kafka-1 (Leader) ✓
Kafka-2 (Follower) ✓
Kafka-3 (Follower) ✓

After (Kafka-1 Down):
Kafka-1 ✗
Kafka-2 (New Leader) ✓  ◄── 자동 Leader 선출
Kafka-3 (Follower) ✓
```
- **결과**: 서비스 정상 운영 (다운타임 없음)
- ISR이 2개 이상 유지되어 계속 쓰기 가능

### 시나리오 2: 2개 브로커 장애
```
Before:
Kafka-1 (Leader) ✓
Kafka-2 (Follower) ✓
Kafka-3 (Follower) ✓

After (Kafka-1, Kafka-2 Down):
Kafka-1 ✗
Kafka-2 ✗
Kafka-3 (Leader) ✓
```
- **결과**: 읽기는 가능하지만 쓰기 차단
- ISR이 1개이므로 Min ISR(2) 조건 미달
- Producer는 `NotEnoughReplicasException` 발생
- **복구 시**: 브로커 재시작 후 자동으로 sync 및 정상 운영

### 시나리오 3: 3개 브로커 모두 장애
- **결과**: Kafka 클러스터 전체 다운
- 데이터는 보존되며 브로커 재시작 시 복구

## 성능 최적화

### Producer 설정
```yaml
acks: all                          # 모든 ISR 확인 (안정성 우선)
retries: 3                         # 재시도 3회
enable.idempotence: true           # 중복 방지
compression.type: snappy           # 압축 (네트워크 대역폭 절약)
linger.ms: 10                      # 배치 처리 (10ms 대기)
batch.size: 32768                  # 32KB 배치
max.in.flight.requests.per.connection: 5
```

### Consumer 설정
```yaml
enable-auto-commit: false          # 수동 커밋 (정확한 offset 관리)
auto-offset-reset: earliest        # 처음부터 읽기
ack-mode: manual                   # 수동 acknowledgment
```

## 모니터링 지표

### 중요 메트릭
1. **Under-Replicated Partitions**
   - 0이어야 정상
   - 1 이상이면 레플리카 sync 문제

2. **ISR Shrink/Expand Rate**
   - ISR 변동 빈도 모니터링
   - 잦은 변동은 네트워크 또는 브로커 성능 문제 의미

3. **Producer Request Rate**
   - 초당 요청 수 모니터링

4. **Consumer Lag**
   - Consumer가 얼마나 뒤쳐져 있는지
   - 0에 가까울수록 좋음

## 운영 가이드

### 클러스터 시작
```bash
docker-compose up -d
```

### 클러스터 상태 확인
```bash
# 토픽 목록 확인
docker exec -it hhplus-ecommerce-kafka-1 kafka-topics --bootstrap-server localhost:9092 --list

# 토픽 상세 정보 (레플리카 상태 확인)
docker exec -it hhplus-ecommerce-kafka-1 kafka-topics --bootstrap-server localhost:9092 --describe --topic coupon-events

# 컨슈머 그룹 상태
docker exec -it hhplus-ecommerce-kafka-1 kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group stock-history-consumer-group
```

### 브로커 개별 재시작
```bash
# Kafka-1 재시작
docker-compose restart kafka-1

# 다른 브로커들은 계속 운영됨 (무중단)
```

### 전체 클러스터 재시작
```bash
docker-compose down
docker-compose up -d
```

## 장점

### 1. 고가용성 (High Availability)
- 최대 2개 브로커 장애까지 서비스 계속 운영
- 자동 Leader 선출로 다운타임 최소화

### 2. 데이터 내구성 (Durability)
- 3-way 레플리케이션으로 데이터 손실 방지
- Min ISR 2로 최소 2개 브로커에 안전하게 저장

### 3. 확장성 (Scalability)
- 파티션 3개로 병렬 처리 가능
- Consumer Group으로 수평 확장

### 4. 무중단 유지보수
- Rolling restart 가능
- 한 번에 1개 브로커씩 재시작하며 서비스 유지

## 프로덕션 환경 고려사항

### 하드웨어 요구사항
- **CPU**: 브로커당 최소 4 cores
- **메모리**: 브로커당 최소 8GB (힙 4GB)
- **디스크**: SSD 권장 (IOPS 중요)
- **네트워크**: 1Gbps 이상

### 네트워크 레이턴시
- 브로커 간 레이턴시 < 10ms 권장
- 같은 데이터센터 또는 같은 가용 영역(AZ) 배치

### 백업 전략
- Kafka 데이터 볼륨 정기 백업
- 설정 파일 버전 관리

## 참고 자료
- [Kafka Replication Design](https://kafka.apache.org/documentation/#replication)
- [Kafka Producer Configuration](https://kafka.apache.org/documentation/#producerconfigs)
- [Kafka Consumer Configuration](https://kafka.apache.org/documentation/#consumerconfigs)
