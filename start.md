# HHPlus E-commerce 프로젝트 시작 가이드

## 목차
1. [사전 요구사항](#사전-요구사항)
2. [프로젝트 개요](#프로젝트-개요)
3. [환경 설정](#환경-설정)
4. [데이터베이스 설정](#데이터베이스-설정)
5. [애플리케이션 실행](#애플리케이션-실행)
6. [API 테스트](#api-테스트)
7. [테스트 실행](#테스트-실행)
8. [문제 해결](#문제-해결)

---

## 사전 요구사항

프로젝트를 실행하기 전에 다음 소프트웨어가 설치되어 있어야 합니다:

### 필수 설치 항목
- **Java 17** 이상
  ```bash
  java -version
  # 출력 예: openjdk version "17.0.x"
  ```

- **Docker & Docker Compose**
  ```bash
  docker --version
  docker compose version
  ```

### 선택 설치 항목
- **Git** (프로젝트 클론용)
- **Postman** 또는 **Insomnia** (API 테스트용, Swagger UI로도 가능)
- **MySQL Workbench** (데이터베이스 GUI 관리 도구)

---

## 프로젝트 개요

### 기술 스택
- **Backend Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle
- **Language**: Java 17
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA (Hibernate)
- **API Documentation**: Swagger/OpenAPI 3.0
- **Test Coverage**: JaCoCo

### 주요 기능
- 사용자 포인트 관리
- 상품 관리 및 재고 관리
- 장바구니 기능
- 쿠폰 발급 및 관리
- 주문 및 결제 처리
- 인기 상품 조회

---

## 환경 설정

### 1. 프로젝트 클론 (선택)

```bash
# GitHub에서 클론하는 경우
git clone <repository-url>
cd week2
```

### 2. 프로젝트 구조 확인

```
week2/
├── src/
│   ├── main/
│   │   ├── java/com/hhplus/ecommerce/    # 애플리케이션 코드
│   │   └── resources/
│   │       └── application.yml            # 설정 파일
│   └── test/                              # 테스트 코드
├── docker-compose.yml                     # Docker 설정
├── init-db/
│   └── 01-schema.sql                      # DB 초기화 스크립트
├── build.gradle                           # Gradle 빌드 설정
└── start.md                               # 이 파일
```

---

## 데이터베이스 설정

### Docker Compose를 사용한 MySQL 실행

#### 1. Docker Compose 파일 확인

`docker-compose.yml` 파일에 다음과 같이 MySQL 설정이 되어 있습니다:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: hhplus-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: ecommerce
      MYSQL_USER: hhplus
      MYSQL_PASSWORD: hhplus123
    ports:
      - "9910:9910"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-db:/docker-entrypoint-initdb.d
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mysql-data:
```

#### 2. MySQL 컨테이너 실행

```bash
# MySQL 컨테이너 시작 (백그라운드 실행)
docker compose up -d

# 로그 확인
docker compose logs -f mysql

# 컨테이너 상태 확인
docker compose ps
```

#### 3. 데이터베이스 초기화 확인

컨테이너가 시작되면 `init-db/01-schema.sql` 파일이 자동으로 실행되어 다음 테이블들이 생성됩니다:

- USERS (사용자)
- POINT_HISTORY (포인트 내역)
- PRODUCT (상품)
- PRODUCT_OPTIONS (상품 옵션)
- COUPONS (쿠폰)
- COUPON_HISTORY (쿠폰 발급 내역)
- CART (장바구니)
- ORDERS (주문)
- ORDER_ITEMS (주문 상품)
- PAYMENTS (결제)
- EXTERNAL_SYNC_LOG (외부 시스템 동기화 로그)
- PRODUCT_STATISTICS (상품 통계)

#### 4. MySQL 컨테이너 접속 (확인용)

```bash
# MySQL 컨테이너 내부로 접속
docker exec -it hhplus-mysql mysql -uhhplus -phhplus123 ecommerce

# 테이블 확인
SHOW TABLES;

# 테이블 구조 확인 예시
DESC USERS;

# 종료
exit;
```

#### 5. Docker 컨테이너 관리 명령어

```bash
# 컨테이너 중지
docker compose stop

# 컨테이너 재시작
docker compose restart

# 컨테이너 중지 및 삭제
docker compose down

# 컨테이너 및 볼륨 모두 삭제 (데이터 초기화)
docker compose down -v

# 컨테이너 로그 확인
docker compose logs mysql

# 실시간 로그 확인
docker compose logs -f mysql
```

### 데이터베이스 연결 정보

애플리케이션에서 사용하는 데이터베이스 연결 정보는 `src/main/resources/application.yml`에 설정되어 있습니다:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:9910/ecommerce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: hhplus
    password: hhplus123
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### MySQL Workbench로 데이터베이스 연결

MySQL Workbench를 사용하면 GUI를 통해 데이터베이스를 편리하게 관리할 수 있습니다.

#### 1. MySQL Workbench 설치

공식 웹사이트에서 MySQL Workbench를 다운로드하여 설치합니다:
- [MySQL Workbench 다운로드](https://dev.mysql.com/downloads/workbench/)

#### 2. 새 연결 생성

1. MySQL Workbench를 실행합니다
2. 메인 화면에서 `MySQL Connections` 옆의 `+` 버튼을 클릭합니다
3. 다음 정보를 입력합니다:

**연결 설정 정보:**
```
Connection Name: HHPlus E-commerce (또는 원하는 이름)
Connection Method: Standard (TCP/IP)
Hostname: 127.0.0.1 또는 localhost
Port: 9910
Username: hhplus
Password: [Store in Keychain/Vault 클릭 후 'hhplus123' 입력]
Default Schema: ecommerce
```

#### 3. 연결 테스트

1. 설정 화면에서 `Test Connection` 버튼을 클릭합니다
2. 비밀번호 입력 창이 나타나면 `hhplus123`을 입력합니다
3. "Successfully made the MySQL connection" 메시지가 나타나면 성공입니다
4. `OK` 버튼을 클릭하여 연결을 저장합니다

#### 4. 데이터베이스 접속

1. 메인 화면에서 생성한 연결을 더블클릭합니다
2. 비밀번호를 저장했다면 자동으로 연결되고, 아니면 비밀번호를 입력합니다
3. 왼쪽 `Schemas` 패널에서 `ecommerce` 데이터베이스를 확인할 수 있습니다

#### 5. MySQL Workbench 주요 기능

**테이블 조회:**
```sql
-- 왼쪽 패널에서 ecommerce > Tables > 테이블명을 우클릭
-- "Select Rows - Limit 1000" 선택
```

**쿼리 실행:**
```sql
-- 메뉴에서 File > New Query Tab 선택 (또는 Ctrl+T / Cmd+T)
-- SQL 쿼리 작성 후 번개 아이콘 클릭 또는 Ctrl+Enter

-- 예시 쿼리
SELECT * FROM USERS;
SELECT * FROM PRODUCT;
SELECT * FROM ORDERS WHERE user_id = 1;
```

**테이블 구조 확인:**
```sql
-- 왼쪽 패널에서 테이블명 옆의 렌치 아이콘 클릭
-- 또는 쿼리 실행
DESC USERS;
SHOW CREATE TABLE PRODUCT;
```

**데이터 추가/수정:**
```sql
-- 새 쿼리 탭에서 SQL 작성
INSERT INTO USERS (username, point, role) VALUES ('testuser', 10000, 'USER');
UPDATE USERS SET point = 20000 WHERE id = 1;
DELETE FROM CART WHERE id = 1;
```

**ER 다이어그램 확인:**
```
1. 메뉴에서 Database > Reverse Engineer 선택
2. 연결 선택 후 Next
3. ecommerce 선택 후 Next
4. 테이블 선택 후 Execute
5. ER 다이어그램이 자동으로 생성됩니다
```

#### 6. Root 계정으로 연결 (관리자 권한 필요 시)

관리자 권한이 필요한 작업의 경우 root 계정으로 연결할 수 있습니다:

```
Connection Name: HHPlus E-commerce (Root)
Hostname: 127.0.0.1
Port: 9910
Username: root
Password: rootpassword
```

#### 7. 연결 문제 해결

**연결 실패 시 체크리스트:**

1. Docker 컨테이너 실행 확인:
```bash
docker compose ps
# hhplus-mysql 컨테이너가 "Up" 상태여야 함
```

2. MySQL 포트 확인:
```bash
# Mac/Linux
lsof -i :9910

# Windows
netstat -ano | findstr :9910
```

3. 방화벽 설정 확인:
   - 로컬 연결이므로 대부분 문제없지만, 방화벽이 9910 포트를 차단하지 않는지 확인

4. 비밀번호 확인:
   - 일반 사용자: `hhplus` / `hhplus123`
   - Root 사용자: `root` / `rootpassword`

5. Docker 컨테이너 재시작:
```bash
docker compose restart mysql
# 또는
docker compose down && docker compose up -d
```

#### 8. 유용한 쿼리 모음

MySQL Workbench에서 다음 쿼리들을 실행하여 데이터를 확인할 수 있습니다:

```sql
-- 모든 테이블 목록 조회
SHOW TABLES;

-- 테이블별 레코드 수 확인
SELECT 'USERS' AS table_name, COUNT(*) AS count FROM USERS
UNION ALL
SELECT 'PRODUCT', COUNT(*) FROM PRODUCT
UNION ALL
SELECT 'ORDERS', COUNT(*) FROM ORDERS
UNION ALL
SELECT 'CART', COUNT(*) FROM CART;

-- 사용자별 포인트 현황
SELECT id, username, point, created_at FROM USERS ORDER BY point DESC;

-- 상품별 재고 현황
SELECT
    p.id,
    p.product_name,
    po.option_type,
    po.stock,
    po.additional_price
FROM PRODUCT p
LEFT JOIN PRODUCT_OPTIONS po ON p.id = po.product_id
ORDER BY p.id, po.id;

-- 주문 현황 (사용자명 포함)
SELECT
    o.id AS order_id,
    u.username,
    o.status,
    o.discount_amount,
    o.point_discount,
    o.ordered_at
FROM ORDERS o
JOIN USERS u ON o.user_id = u.id
ORDER BY o.created_at DESC
LIMIT 50;

-- 쿠폰 발급 현황
SELECT
    c.coupon_name,
    c.discount_type,
    c.discount_amount,
    c.stock AS remaining_stock,
    COUNT(ch.id) AS issued_count
FROM COUPONS c
LEFT JOIN COUPON_HISTORY ch ON c.id = ch.coupon_id
GROUP BY c.id, c.coupon_name, c.discount_type, c.discount_amount, c.stock;
```

---

## 애플리케이션 실행

### 방법 1: Gradle을 사용한 실행 (권장)

```bash
# 프로젝트 루트 디렉토리에서 실행
./gradlew bootRun

# Windows의 경우
gradlew.bat bootRun
```

### 방법 2: 빌드 후 JAR 파일 실행

```bash
# 1. 프로젝트 빌드
./gradlew clean build

# 2. JAR 파일 실행
java -jar build/libs/hhplus-ecommerce-0.0.1-SNAPSHOT.jar
```

### 방법 3: IDE에서 실행

#### IntelliJ IDEA
1. 프로젝트를 IntelliJ IDEA에서 엽니다
2. `src/main/java/com/hhplus/ecommerce/HhplusApplication.java` 파일을 찾습니다
3. 파일을 우클릭하고 `Run 'HhplusApplication'`을 선택합니다

#### Eclipse
1. 프로젝트를 Eclipse에서 import합니다
2. `HhplusApplication.java` 파일을 찾습니다
3. 우클릭 후 `Run As > Java Application` 선택

### 애플리케이션 시작 확인

애플리케이션이 성공적으로 시작되면 다음과 같은 로그가 출력됩니다:

```
Started HhplusApplication in X.XXX seconds
```

기본 포트는 **8080**입니다. 브라우저에서 다음 URL로 접속하여 확인할 수 있습니다:

```
http://localhost:8080
```

---

## API 테스트

### Swagger UI 사용

애플리케이션이 실행되면 Swagger UI를 통해 API를 테스트할 수 있습니다:

```
http://localhost:8080/swagger-ui.html
```

Swagger UI에서 다음을 확인할 수 있습니다:
- 모든 API 엔드포인트 목록
- API 요청/응답 스키마
- 직접 API 테스트 실행

### 주요 API 엔드포인트

#### 1. 사용자 API

```bash
# 사용자 조회
GET http://localhost:8080/api/users/{userId}

# 사용자 포인트 조회
GET http://localhost:8080/api/users/{userId}/point

# 포인트 충전
POST http://localhost:8080/api/users/{userId}/point/charge
Content-Type: application/json

{
  "amount": 10000
}
```

#### 2. 상품 API

```bash
# 상품 목록 조회
GET http://localhost:8080/api/products

# 상품 상세 조회
GET http://localhost:8080/api/products/{productId}

# 인기 상품 조회 (최근 3일간 판매량 기준 Top 5)
GET http://localhost:8080/api/products/popular?days=3
```

#### 3. 장바구니 API

```bash
# 장바구니 조회
GET http://localhost:8080/api/carts?userId={userId}

# 장바구니에 상품 추가
POST http://localhost:8080/api/carts
Content-Type: application/json

{
  "userId": 1,
  "productId": 1,
  "productOptionId": 1,
  "quantity": 2
}

# 장바구니 상품 삭제
DELETE http://localhost:8080/api/carts/{cartId}
```

#### 4. 쿠폰 API

```bash
# 발급 가능한 쿠폰 목록 조회
GET http://localhost:8080/api/coupons/available

# 쿠폰 발급
POST http://localhost:8080/api/coupons/issue
Content-Type: application/json

{
  "userId": 1,
  "couponId": 1
}

# 사용자 쿠폰 조회
GET http://localhost:8080/api/coupons/user/{userId}
```

#### 5. 주문 API

```bash
# 주문 생성
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "productOptionId": 1,
      "quantity": 2
    }
  ],
  "couponHistoryId": 1,
  "pointDiscount": 1000
}

# 주문 조회
GET http://localhost:8080/api/orders/{orderId}

# 사용자 주문 목록 조회
GET http://localhost:8080/api/orders/user/{userId}
```

#### 6. 결제 API

```bash
# 결제 실행
POST http://localhost:8080/api/payments
Content-Type: application/json

{
  "orderId": 1,
  "paymentMethod": "CARD"
}

# 결제 조회
GET http://localhost:8080/api/payments/{paymentId}
```

### cURL을 사용한 테스트 예시

```bash
# 사용자 포인트 충전
curl -X POST http://localhost:8080/api/users/1/point/charge \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000}'

# 인기 상품 조회
curl -X GET "http://localhost:8080/api/products/popular?days=3"

# 장바구니에 상품 추가
curl -X POST http://localhost:8080/api/carts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "productOptionId": 1,
    "quantity": 2
  }'
```

---

## 테스트 실행

### 전체 테스트 실행

```bash
# 모든 테스트 실행
./gradlew test

# 테스트 결과 확인
# build/reports/tests/test/index.html 파일을 브라우저로 열기
```

### 테스트 커버리지 확인 (JaCoCo)

```bash
# 테스트 실행 및 커버리지 리포트 생성
./gradlew test jacocoTestReport

# 커버리지 리포트 확인
# build/reports/jacoco/index.html 파일을 브라우저로 열기
```

### 특정 테스트 클래스 실행

```bash
# 특정 테스트 클래스만 실행
./gradlew test --tests CartControllerTest

# 특정 테스트 메서드만 실행
./gradlew test --tests CartControllerTest.testAddToCart
```

### 통합 테스트 실행

프로젝트에는 다음과 같은 통합 테스트가 포함되어 있습니다:
- CartControllerTest
- CouponControllerTest
- OrderControllerTest
- PaymentControllerTest

```bash
# 통합 테스트만 실행
./gradlew test --tests "*ControllerTest"
```

---

## 문제 해결

### 1. 포트 충돌 문제

**증상**: `Port 8080 is already in use` 에러

**해결방법**:
```bash
# 포트 사용 중인 프로세스 확인 (Mac/Linux)
lsof -i :8080

# 포트 사용 중인 프로세스 확인 (Windows)
netstat -ano | findstr :8080

# 프로세스 종료 후 재시작
# 또는 application.yml에서 포트 변경
server:
  port: 8081
```

### 2. MySQL 연결 실패

**증상**: `Communications link failure` 또는 `Access denied for user` 에러

**해결방법**:
```bash
# 1. MySQL 컨테이너 상태 확인
docker compose ps

# 2. MySQL 컨테이너가 실행 중이 아니면 시작
docker compose up -d

# 3. MySQL 로그 확인
docker compose logs mysql

# 4. MySQL 연결 테스트
docker exec -it hhplus-mysql mysql -uhhplus -phhplus123 ecommerce

# 5. 데이터베이스와 권한 확인
SHOW DATABASES;
SELECT user, host FROM mysql.user WHERE user='hhplus';
```

### 3. 데이터베이스 스키마 문제

**증상**: `Table doesn't exist` 에러

**해결방법**:
```bash
# 1. 컨테이너 및 볼륨 삭제 (데이터 초기화)
docker compose down -v

# 2. 컨테이너 재시작 (스키마 자동 재생성)
docker compose up -d

# 3. 로그 확인
docker compose logs -f mysql

# 4. 테이블 생성 확인
docker exec -it hhplus-mysql mysql -uhhplus -phhplus123 ecommerce -e "SHOW TABLES;"
```

### 4. 빌드 실패

**증상**: Gradle 빌드 중 오류 발생

**해결방법**:
```bash
# 1. Gradle 캐시 정리
./gradlew clean

# 2. Gradle Wrapper 업데이트
./gradlew wrapper --gradle-version=8.5

# 3. 의존성 다시 다운로드
./gradlew build --refresh-dependencies

# 4. Java 버전 확인
java -version  # Java 17 이상이어야 함
```

### 5. Docker 관련 문제

**증상**: Docker 명령어가 작동하지 않음

**해결방법**:
```bash
# Docker 서비스 상태 확인
docker info

# Docker 재시작 (Mac)
# Docker Desktop 앱을 재시작

# Docker 재시작 (Linux)
sudo systemctl restart docker

# Docker Compose 버전 확인
docker compose version  # v2.x 이상 권장
```

### 6. 메모리 부족 문제

**증상**: `OutOfMemoryError` 발생

**해결방법**:
```bash
# JVM 메모리 옵션 설정
export JAVA_OPTS="-Xms512m -Xmx2048m"
./gradlew bootRun

# 또는 직접 JAR 실행 시
java -Xms512m -Xmx2048m -jar build/libs/hhplus-ecommerce-0.0.1-SNAPSHOT.jar
```

---

## 개발 환경 설정

### 개발 모드로 실행

```bash
# 개발 모드 (자동 재시작 활성화)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 로그 레벨 변경

`application.yml` 파일에서 로그 레벨을 조정할 수 있습니다:

```yaml
logging:
  level:
    root: INFO
    com.hhplus: DEBUG           # 애플리케이션 로그
    org.hibernate.SQL: DEBUG    # SQL 쿼리 로그
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # SQL 파라미터 로그
```

### 데이터베이스 초기 데이터 입력

프로젝트에는 스키마만 자동 생성됩니다. 테스트 데이터를 입력하려면:

```bash
# MySQL 접속
docker exec -it hhplus-mysql mysql -uhhplus -phhplus123 ecommerce

# 샘플 데이터 입력 예시
INSERT INTO USERS (username, point, role) VALUES ('testuser', 10000, 'USER');
INSERT INTO PRODUCT (created_user_id, product_name, content, price) VALUES (1, '테스트 상품', '상품 설명', 50000);
```

---

## 프로젝트 종료

### 애플리케이션 종료

```bash
# Gradle bootRun 실행 중인 경우
Ctrl + C

# JAR 파일 실행 중인 경우
Ctrl + C

# 또는 프로세스 찾아서 종료
ps aux | grep hhplus
kill -9 <PID>
```

### MySQL 컨테이너 종료

```bash
# 컨테이너 중지 (데이터 보존)
docker compose stop

# 컨테이너 삭제 (데이터 보존)
docker compose down

# 컨테이너 및 데이터 모두 삭제
docker compose down -v
```

---

## 추가 리소스

### 프로젝트 문서
- [동시성 제어 분석](README.md)
- [데이터 모델 문서](docs/data-models.md)
- API 문서: `http://localhost:8080/swagger-ui.html` (실행 후)

### 기술 스택 공식 문서
- [Spring Boot 3.2.0 Documentation](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [MySQL 8.0 Documentation](https://dev.mysql.com/doc/refman/8.0/en/)
- [Gradle Documentation](https://docs.gradle.org/)
- [Docker Documentation](https://docs.docker.com/)

---

## 빠른 시작 요약

처음 프로젝트를 실행하는 경우 다음 순서대로 진행하세요:

```bash
# 1. MySQL 컨테이너 시작
docker compose up -d

# 2. 컨테이너 상태 확인
docker compose ps

# 3. 애플리케이션 실행
./gradlew bootRun

# 4. 브라우저에서 Swagger UI 접속
# http://localhost:8080/swagger-ui.html

# 5. API 테스트 진행
```

종료할 때:
```bash
# 1. 애플리케이션 종료 (Ctrl + C)

# 2. MySQL 컨테이너 중지
docker compose down
```

---

## 문의 및 지원

문제가 발생하거나 질문이 있는 경우:
1. 먼저 [문제 해결](#문제-해결) 섹션을 확인하세요
2. 프로젝트 이슈 트래커를 확인하세요
3. 로그 파일을 확인하세요 (`docker compose logs`, 애플리케이션 콘솔 출력)