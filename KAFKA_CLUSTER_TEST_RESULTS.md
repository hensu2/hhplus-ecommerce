# Kafka 클러스터 및 레플리케이션 테스트 결과

## 테스트 개요
- **테스트 날짜**: 2025-12-19
- **클러스터 구성**: 3-broker Kafka cluster (kafka-1, kafka-2, kafka-3)
- **레플리케이션 설정**: Replication Factor 3, Min ISR 2

## 1. 클러스터 구성 검증

### 브로커 상태
```
NAMES                      STATUS
hhplus-ecommerce-kafka-1   Up (healthy)
hhplus-ecommerce-kafka-2   Up (healthy)
hhplus-ecommerce-kafka-3   Up (healthy)
```

### 주요 설정
```yaml
# 각 브로커 설정
KAFKA_LISTENERS: INTERNAL://0.0.0.0:190XX,EXTERNAL://0.0.0.0:90XX
KAFKA_ADVERTISED_LISTENERS:
  - INTERNAL: kafka-X:190XX (브로커 간 통신)
  - EXTERNAL: localhost:90XX (클라이언트 연결)
KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL

# 레플리케이션 설정
KAFKA_DEFAULT_REPLICATION_FACTOR: 3
KAFKA_MIN_INSYNC_REPLICAS: 2
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
```

## 2. 토픽 레플리케이션 검증

### stock-events 토픽
```
Topic: stock-events
PartitionCount: 3
ReplicationFactor: 3
Configs: compression.type=snappy,min.insync.replicas=2,retention.ms=604800000

Partition 0: Leader=3, Replicas=[3,1,2], ISR=[3,1,2] ✓
Partition 1: Leader=1, Replicas=[1,2,3], ISR=[1,2,3] ✓
Partition 2: Leader=2, Replicas=[2,3,1], ISR=[2,3,1] ✓
```

**검증 결과**:
- ✓ 모든 파티션이 3개의 레플리카 보유
- ✓ 모든 레플리카가 In-Sync Replicas (ISR)에 포함
- ✓ 리더가 브로커 간 균등 분배 (broker 1, 2, 3)

### order-events 토픽
```
Topic: order-events
PartitionCount: 3
ReplicationFactor: 3
Configs: compression.type=snappy,min.insync.replicas=2,retention.ms=604800000

Partition 0: Leader=2, Replicas=[2,3,1], ISR=[2,3,1] ✓
Partition 1: Leader=3, Replicas=[3,1,2], ISR=[3,1,2] ✓
Partition 2: Leader=1, Replicas=[1,2,3], ISR=[1,2,3] ✓
```

**검증 결과**:
- ✓ 모든 파티션이 3개의 레플리카 보유
- ✓ 모든 레플리카가 ISR에 포함
- ✓ 리더가 브로커 간 균등 분배

### DLQ 토픽 (stock-events-dlq)
```
Topic: stock-events-dlq
PartitionCount: 1
ReplicationFactor: 3
Configs: min.insync.replicas=2,retention.ms=2592000000

Partition 0: Leader=1, Replicas=[1,2,3], ISR=[1,2,3] ✓
```

**검증 결과**:
- ✓ DLQ 토픽도 레플리케이션 팩터 3 적용
- ✓ 30일 보관 정책 (에러 분석용)

## 3. 브로커 장애 시나리오 테스트

### 시나리오: 1개 브로커 중단 (kafka-1 stop)

#### 장애 발생 전
```
Partition 0: Leader=3, ISR=[3,1,2]
Partition 1: Leader=1, ISR=[1,2,3]  ← kafka-1이 리더
Partition 2: Leader=2, ISR=[2,3,1]
```

#### 장애 발생 후 (kafka-1 stopped)
```
Partition 0: Leader=3, ISR=[3,2]    ← broker 1 제거됨
Partition 1: Leader=2, ISR=[2,3]    ← 리더 자동 전환: 1→2
Partition 2: Leader=2, ISR=[2,3]    ← broker 1 제거됨
```

**관찰 결과**:
1. ✓ **자동 리더 선출**: Partition 1의 리더가 broker 1 → broker 2로 즉시 전환
2. ✓ **ISR 자동 조정**: 장애 브로커(broker 1)가 모든 ISR에서 자동 제거
3. ✓ **Min ISR 충족**: 모든 파티션이 여전히 2개의 레플리카 유지 (min.insync.replicas=2)
4. ✓ **서비스 무중단**: 클러스터가 정상 작동 계속
5. ✓ **쓰기/읽기 가능**: 애플리케이션이 계속해서 메시지 produce/consume 가능

#### 복구 후 (kafka-1 restarted)
```
Partition 0: Leader=3, ISR=[3,2,1]  ← broker 1 복귀
Partition 1: Leader=2, ISR=[2,3,1]  ← broker 1 복귀 (리더는 유지)
Partition 2: Leader=2, ISR=[2,3,1]  ← broker 1 복귀
```

**복구 결과**:
1. ✓ **자동 동기화**: Broker 1이 재시작 후 자동으로 데이터 sync
2. ✓ **ISR 복원**: Broker 1이 모든 파티션의 ISR에 재가입
3. ✓ **전체 레플리카 복구**: 모든 파티션이 다시 3개의 in-sync 레플리카 보유
4. ✓ **데이터 무손실**: 모든 메시지 보존 확인
5. ✓ **리더 안정성**: 리더는 불필요하게 변경되지 않음 (안정성 유지)

## 4. 고가용성 검증 요약

### ✓ 검증된 기능
1. **고가용성 (High Availability)**
   - 1개 브로커 장애 시 무중단 서비스 제공
   - 자동 리더 선출 (수 초 내 완료)
   - 읽기/쓰기 작업 지속 가능

2. **데이터 내구성 (Durability)**
   - 3-way 레플리케이션으로 데이터 보호
   - Min ISR 2 정책으로 안전한 쓰기 보장
   - 브로커 재시작 후 데이터 완전 복구

3. **자동 복구 (Auto Recovery)**
   - 장애 브로커 자동 감지 및 ISR 제거
   - 재시작 시 자동 동기화 및 ISR 복귀
   - 운영자 개입 없이 자동 복구

4. **부하 분산 (Load Balancing)**
   - 리더가 브로커 간 균등 분배
   - 파티션 별 독립적인 리더 선출
   - 3개 파티션으로 병렬 처리 가능

### ✓ 내결함성 (Fault Tolerance)
```
┌─────────────────────┬──────────────┬──────────────┐
│  장애 시나리오        │ 읽기 가능     │ 쓰기 가능     │
├─────────────────────┼──────────────┼──────────────┤
│ 정상 (3 brokers)    │ ✓ 가능       │ ✓ 가능       │
│ 1 broker down       │ ✓ 가능       │ ✓ 가능       │
│ 2 brokers down      │ ✓ 가능       │ ✗ 차단*      │
│ 3 brokers down      │ ✗ 불가       │ ✗ 불가       │
└─────────────────────┴──────────────┴──────────────┘
* ISR=1 < Min ISR(2) 조건으로 쓰기 차단
```

## 5. 성능 및 최적화 설정

### Producer 설정
```yaml
acks: all                    # 모든 ISR 확인 (최고 안정성)
retries: 3                   # 실패 시 3회 재시도
enable.idempotence: true     # 중복 메시지 방지
compression.type: snappy     # 압축으로 네트워크 절약
linger.ms: 10                # 10ms 배치 처리
batch.size: 32768            # 32KB 배치
```

### Consumer 설정
```yaml
enable-auto-commit: false    # 수동 커밋 (정확한 offset 관리)
auto-offset-reset: earliest  # 처음부터 읽기
ack-mode: manual             # 수동 acknowledgment
```

## 6. 토픽 목록

### 생성된 토픽
```
coupon-events              (3 partitions, RF=3, Min ISR=2)
coupon-events-dlq          (1 partition,  RF=3, Min ISR=2)
order-events               (3 partitions, RF=3, Min ISR=2)
order-events-dlq           (1 partition,  RF=3, Min ISR=2)
order-events-retry-2000    (자동 생성)
order-events-retry-4000    (자동 생성)
stock-events               (3 partitions, RF=3, Min ISR=2)
stock-events-dlq           (1 partition,  RF=3, Min ISR=2)
stock-events-retry-2000    (자동 생성)
stock-events-retry-4000    (자동 생성)
```

## 7. 운영 가이드

### 클러스터 상태 확인
```bash
# 브로커 상태 확인
docker ps --filter "name=kafka"

# 토픽 목록 확인
docker exec hhplus-ecommerce-kafka-1 kafka-topics --bootstrap-server kafka-1:19092 --list

# 토픽 상세 정보 확인 (레플리카 상태)
docker exec hhplus-ecommerce-kafka-1 kafka-topics --bootstrap-server kafka-1:19092 --describe --topic stock-events
```

### 브로커 재시작 (Rolling Restart)
```bash
# 1개씩 재시작하여 무중단 유지보수
docker restart hhplus-ecommerce-kafka-1
sleep 30  # ISR 복구 대기
docker restart hhplus-ecommerce-kafka-2
sleep 30
docker restart hhplus-ecommerce-kafka-3
```

## 8. 결론

### 성공적으로 검증된 사항
1. ✓ 3-broker Kafka 클러스터 정상 구축
2. ✓ 모든 토픽이 Replication Factor 3으로 생성
3. ✓ Min ISR 2 정책으로 안전한 데이터 쓰기 보장
4. ✓ 1개 브로커 장애 시 자동 Failover 동작 확인
5. ✓ 브로커 복구 시 자동 ISR 복원 확인
6. ✓ 무중단 서비스 제공 검증
7. ✓ 데이터 무손실 검증

### 프로덕션 준비 상태
이 Kafka 클러스터는 다음과 같은 프로덕션 환경 요구사항을 충족합니다:
- **고가용성**: 최대 1개 브로커 장애까지 무중단 운영
- **데이터 안정성**: 3-way 레플리케이션 및 Min ISR 정책
- **자동 복구**: 운영자 개입 없이 자동 장애 복구
- **확장성**: 파티션을 통한 병렬 처리 및 수평 확장 가능

### 향후 개선 사항
1. 모니터링 설정 (Prometheus + Grafana)
   - Under-Replicated Partitions 모니터링
   - ISR Shrink/Expand Rate 추적
   - Consumer Lag 모니터링
2. 알림 설정 (AlertManager)
   - 브로커 다운 알림
   - ISR < Min ISR 알림
   - Consumer Lag 임계값 초과 알림
3. 백업 전략 수립
   - 주기적인 데이터 볼륨 백업
   - 설정 파일 버전 관리
