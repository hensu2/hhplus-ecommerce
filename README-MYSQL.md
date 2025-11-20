# MySQL 데이터베이스 설정

## Docker Compose로 MySQL 실행

```bash
# MySQL 컨테이너 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f mysql

# MySQL 접속
docker exec -it hhplus-ecommerce-mysql mysql -uhhplus -phhplus1234 ecommerce

# 컨테이너 중지
docker-compose down

# 데이터까지 삭제 (볼륨 삭제)
docker-compose down -v
```

## 데이터베이스 정보

- **Host:** localhost
- **Port:** 9910
- **Database:** ecommerce
- **Username:** hhplus
- **Password:** hhplus1234
- **Root Password:** root1234

## 스키마 정보

스키마는 `docker/init/01-schema.sql`에 정의되어 있으며, Docker Compose 시작 시 자동으로 실행됩니다.

### 테이블 목록
1. users - 사용자
2. point_history - 포인트 내역
3. products - 상품
4. product_options - 상품 옵션
5. coupons - 쿠폰
6. coupon_history - 쿠폰 발급 내역
7. cart - 장바구니
8. orders - 주문
9. order_items - 주문 상세
10. payments - 결제
11. product_statistics - 상품 통계
12. external_sync_log - 외부 동기화 로그

### 인덱스
각 테이블에는 성능 최적화를 위한 인덱스가 설정되어 있습니다:
- Primary Key
- Foreign Key
- 조회 성능을 위한 복합 인덱스
- UNIQUE 제약조건 (필요한 경우)

## 샘플 데이터

`docker/init/02-sample-data.sql`에 샘플 데이터가 포함되어 있습니다:
- 3명의 사용자 (user1, user2, admin)
- 5개의 상품 (아이폰, 갤럭시, 맥북, 에어팟, 애플워치)
- 각 상품별 옵션
- 3개의 쿠폰
- 상품 통계 데이터

## 애플리케이션 설정

`application.yml`에 MySQL 연결 설정이 되어 있습니다:
- JPA Hibernate ddl-auto는 사용하지 않음 (스키마는 init.sql로 관리)
- Connection Pool 설정 (HikariCP)
- MySQL 8 Dialect 사용

## 주의사항

- JPA `ddl-auto`를 사용하지 않으므로 스키마 변경은 `init.sql`을 수정해야 합니다.
- 데이터베이스를 초기화하려면 `docker-compose down -v`로 볼륨까지 삭제 후 재시작하세요.
