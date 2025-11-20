# Controller 아키텍처 설명

## 개요

이 문서는 e-commerce 프로젝트의 Controller 계층 아키텍처와 각 Controller에서 수행되는 작업을 상세히 설명합니다.

## 아키텍처 패턴

### 전체 흐름
```
Client Request
    ↓
Controller (Presentation Layer)
    ↓
UseCase (Application Layer) - Entity 반환
    ↓
Repository (Infrastructure Layer)
    ↓
Database
```

### 책임 분리
- **Controller**: HTTP 요청/응답 처리, Entity → Response DTO 변환
- **UseCase**: 비즈니스 로직 처리, Entity 반환
- **Repository**: 데이터 접근, JPA 연동

---

## 1. UserController

### 역할
사용자 관리 API를 제공하며, UserEntity → UserResponse 변환을 담당합니다.

### 주요 메서드

#### `getUser(Long id)`
```java
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    UserEntity user = getUserUseCase.execute(id);
    UserResponse response = toUserResponse(user);
    return ResponseEntity.ok(response);
}
```

**작업 흐름:**
1. UseCase에서 UserEntity 조회
2. toUserResponse() 메서드로 Entity → Response 변환
3. HTTP 200 OK와 함께 Response 반환

#### `getUsers()`
```java
public ResponseEntity<List<UserResponse>> getUsers() {
    List<UserEntity> users = getUsersUseCase.execute();
    List<UserResponse> responses = users.stream()
            .map(this::toUserResponse)
            .toList();
    return ResponseEntity.ok(responses);
}
```

**작업 흐름:**
1. UseCase에서 List<UserEntity> 조회
2. Stream API로 각 Entity를 Response로 변환
3. HTTP 200 OK와 함께 Response List 반환

#### `toUserResponse(UserEntity user)`
```java
private UserResponse toUserResponse(UserEntity user) {
    return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getPoint(),
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt()
    );
}
```

**변환 로직:**
- Entity의 모든 필드를 Record Response로 변환
- 추가적인 비즈니스 로직 없이 단순 매핑

---

## 2. UserPointController

### 역할
포인트 관리 API를 제공하며, Entity → PointResponse, ChargePointResponse, PointHistoryResponse 변환을 담당합니다.

### 주요 메서드

#### `getPoint(Long userId)`
```java
public PointResponse getPoint(@RequestParam Long userId) {
    UserEntity user = getPointUseCase.execute(userId);
    return new PointResponse(
            user.getId(),
            user.getUsername(),
            user.getPoint().intValue(),
            toLocalDateTime(user.getUpdatedAt())
    );
}
```

**작업 흐름:**
1. UseCase에서 UserEntity 조회
2. Entity → PointResponse 변환
   - Long → int 타입 변환 (intValue())
   - timestamp → LocalDateTime 문자열 변환
3. PointResponse 직접 반환

#### `chargePoint(Long userId, ChargePointRequest request)`
```java
public ResponseEntity<ChargePointResponse> chargePoint(
        @RequestParam Long userId,
        @RequestBody ChargePointRequest request) {
    UserEntity savedUser = chargePointUseCase.execute(userId, request.amount().longValue());
    ChargePointResponse response = new ChargePointResponse(
            savedUser.getId(),
            request.amount(),
            savedUser.getPoint().intValue(),
            "EARN",
            toLocalDateTime(savedUser.getUpdatedAt())
    );
    return ResponseEntity.ok(response);
}
```

**작업 흐름:**
1. Request에서 amount를 Long으로 변환하여 UseCase 호출
2. UseCase에서 포인트 충전 후 UserEntity 반환
3. Entity → ChargePointResponse 변환
   - 충전 금액 (amount), 잔액 (afterBalance), 거래 타입 포함
4. HTTP 200 OK와 함께 Response 반환

#### `getPointHistory(Long userId, int page, int size)`
```java
public PointHistoryResponse getPointHistory(
        @RequestParam Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    List<PointHistoryEntity> historyEntities = getPointHistoryUseCase.execute(userId);

    List<PointHistoryItemResponse> historyItems = historyEntities.stream()
            .map(this::toPointHistoryItemResponse)
            .toList();

    return new PointHistoryResponse(
            historyItems,
            historyItems.size(),
            1,
            size,
            page
    );
}
```

**작업 흐름:**
1. UseCase에서 List<PointHistoryEntity> 조회
2. 각 Entity를 PointHistoryItemResponse로 변환
   - USE 타입인 경우 amount를 음수로 변환
3. 페이징 정보와 함께 PointHistoryResponse 생성

#### `toPointHistoryItemResponse(PointHistoryEntity entity)`
```java
private PointHistoryItemResponse toPointHistoryItemResponse(PointHistoryEntity entity) {
    int amount = entity.getAmount().intValue();
    if ("USE".equals(entity.getTransactionType())) {
        amount = -amount;
    }

    return new PointHistoryItemResponse(
            entity.getId(),
            amount,
            entity.getTransactionType(),
            entity.getDescription(),
            toLocalDateTime(entity.getCreatedAt())
    );
}
```

**변환 로직:**
- USE 타입인 경우 amount를 음수로 표시
- timestamp → LocalDateTime 문자열 변환

#### `toLocalDateTime(Long timestamp)`
```java
private String toLocalDateTime(Long timestamp) {
    return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
    ).toString();
}
```

**변환 로직:**
- Unix timestamp (Long) → LocalDateTime 문자열
- 시스템 기본 시간대 사용

---

## 3. CartController

### 역할
장바구니 관리 API를 제공하며, Entity → CartResponse, AddCartItemResponse, UpdateCartItemResponse 변환을 담당합니다.

### 의존성
```java
private final GetCartUseCase getCartUseCase;
private final AddToCartUseCase addToCartUseCase;
private final UpdateCartItemUseCase updateCartItemUseCase;
private final DeleteCartItemUseCase deleteCartItemUseCase;
private final ProductRepository productRepository;
private final ProductOptionRepository productOptionRepository;
```

**특이사항:**
- ProductRepository, ProductOptionRepository를 직접 의존
- Cart 정보와 Product/Option 정보를 조합하기 위함

### 주요 메서드

#### `getCart(Long userId)`
```java
public CartResponse getCart(@RequestParam Long userId) {
    List<CartEntity> cartEntities = getCartUseCase.execute(userId);

    List<CartItemResponse> items = cartEntities.stream()
            .map(this::toCartItemResponse)
            .toList();

    int totalAmount = items.stream()
            .mapToInt(CartItemResponse::totalPrice)
            .sum();

    return new CartResponse(items, totalAmount);
}
```

**작업 흐름:**
1. UseCase에서 List<CartEntity> 조회
2. 각 CartEntity를 CartItemResponse로 변환
   - Product, ProductOption 정보 조회 및 조합
3. 전체 금액 계산
4. CartResponse 생성 및 반환

#### `addToCart(Long userId, AddToCartRequest request)`
```java
public ResponseEntity<AddCartItemResponse> addToCart(
        @RequestParam Long userId,
        @RequestBody AddToCartRequest request) {
    CartEntity savedCart = addToCartUseCase.execute(
            userId,
            request.productId(),
            request.optionId(),
            request.quantity()
    );

    ProductEntity product = productRepository.getOrThrow(savedCart.getProductId());
    ProductOptionEntity option = productOptionRepository.getOrThrow(savedCart.getProductOptionId());
    int unitPrice = (int) (product.getPrice() + option.getAdditionalPrice());

    AddCartItemResponse response = new AddCartItemResponse(
            savedCart.getId(),
            savedCart.getProductId(),
            product.getProductName(),
            savedCart.getProductOptionId(),
            option.getOptionType(),
            savedCart.getQuantity(),
            unitPrice,
            unitPrice * savedCart.getQuantity(),
            toLocalDateTime(savedCart.getCreatedAt())
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**작업 흐름:**
1. UseCase에서 CartEntity 생성 (재고 검증 포함)
2. Product, ProductOption 정보 조회
3. 가격 계산
   - unitPrice = product.price + option.additionalPrice
   - totalPrice = unitPrice * quantity
4. HTTP 201 CREATED와 함께 Response 반환

#### `updateCartItem(Long id, UpdateCartItemRequest request)`
```java
public ResponseEntity<UpdateCartItemResponse> updateCartItem(
        @PathVariable Long id,
        @RequestBody UpdateCartItemRequest request) {
    CartEntity savedCart = updateCartItemUseCase.execute(id, request.quantity());

    ProductEntity product = productRepository.getOrThrow(savedCart.getProductId());
    ProductOptionEntity option = productOptionRepository.getOrThrow(savedCart.getProductOptionId());
    int unitPrice = (int) (product.getPrice() + option.getAdditionalPrice());
    int totalPrice = unitPrice * savedCart.getQuantity();

    UpdateCartItemResponse response = new UpdateCartItemResponse(
            savedCart.getId(),
            savedCart.getProductId(),
            savedCart.getQuantity(),
            totalPrice,
            toLocalDateTime(savedCart.getUpdatedAt())
    );
    return ResponseEntity.ok(response);
}
```

**작업 흐름:**
1. UseCase에서 CartEntity 수량 업데이트 (재고 검증 포함)
2. Product, ProductOption 정보 조회
3. 총 가격 재계산
4. HTTP 200 OK와 함께 Response 반환

#### `toCartItemResponse(CartEntity cart)`
```java
private CartItemResponse toCartItemResponse(CartEntity cart) {
    ProductEntity product = productRepository.getOrThrow(cart.getProductId());
    ProductOptionEntity option = productOptionRepository.getOrThrow(cart.getProductOptionId());
    int unitPrice = (int) (product.getPrice() + option.getAdditionalPrice());
    int totalPrice = unitPrice * cart.getQuantity();
    int stock = option.getStock().intValue();

    return new CartItemResponse(
            cart.getId(),
            cart.getProductId(),
            product.getProductName(),
            cart.getProductOptionId(),
            option.getOptionType(),
            cart.getQuantity(),
            unitPrice,
            totalPrice,
            stock
    );
}
```

**변환 로직:**
- Cart, Product, ProductOption 정보 조합
- 가격 및 재고 정보 계산

---

## 4. ProductController

### 역할
상품 관리 API를 제공하며, Entity → ProductListResponse 변환을 담당합니다.

### 주요 메서드

#### `getProducts(int page, int size)`
```java
public ProductListResponse getProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    List<ProductEntity> productEntities = getProductsUseCase.execute();
    List<ProductResponse> products = productEntities.stream()
            .map(ProductEntity::toProductResponse)
            .toList();

    return new ProductListResponse(
        products,
        products.size(),
        1,
        size,
        page
    );
}
```

**작업 흐름:**
1. UseCase에서 List<ProductEntity> 조회
2. Entity → ProductResponse 변환 (Entity의 toProductResponse() 메서드 사용)
3. 페이징 정보와 함께 ProductListResponse 생성

---

## 5. CouponController

### 역할
쿠폰 관리 API를 제공하며, Entity → CouponListResponse 변환을 담당합니다.

### 주요 메서드

#### `getCoupons()`
```java
public ResponseEntity<List<CouponListResponse>> getCoupons() {
    List<CouponEntity> coupons = getCouponsUseCase.execute();
    List<CouponListResponse> response = coupons.stream()
            .map(CouponEntity::toCouponListResponse)
            .toList();
    return ResponseEntity.ok(response);
}
```

**작업 흐름:**
1. UseCase에서 List<CouponEntity> 조회
2. Entity → CouponListResponse 변환 (Entity의 toCouponListResponse() 메서드 사용)
3. HTTP 200 OK와 함께 Response List 반환

---

## 6. OrderController

### 역할
주문 관리 API를 제공하며, UseCase에서 OrderResponse를 직접 반환받습니다.

### 특이사항
**이 Controller는 Entity → Response 변환을 하지 않습니다.**
- UseCase에서 이미 OrderResponse를 반환
- Controller는 단순히 HTTP 요청/응답만 처리

### 주요 메서드

#### `createOrder(CreateOrderRequest request)`
```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
    OrderResponse response = createOrderUseCase.execute(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**작업 흐름:**
1. UseCase에서 OrderResponse 직접 반환
2. HTTP 201 CREATED와 함께 Response 반환
3. **변환 로직 없음**

#### `cancelOrder(Long orderId)`
```java
@PostMapping("/{orderId}/cancel")
public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId) {
    OrderResponse response = cancelOrderUseCase.execute(orderId);
    return ResponseEntity.ok(response);
}
```

**작업 흐름:**
1. UseCase에서 주문 취소 후 OrderResponse 반환
2. HTTP 200 OK와 함께 Response 반환

#### `completeOrder(Long orderId)`
```java
@PostMapping("/{orderId}/complete")
public ResponseEntity<OrderResponse> completeOrder(@PathVariable Long orderId) {
    OrderResponse response = completeOrderUseCase.execute(orderId);
    return ResponseEntity.ok(response);
}
```

**작업 흐름:**
1. UseCase에서 주문 완료 처리 후 OrderResponse 반환
2. HTTP 200 OK와 함께 Response 반환

---

## 7. PaymentController

### 역할
결제 관리 API를 제공하며, UseCase에서 PaymentResponse를 직접 반환받습니다.

### 특이사항
**이 Controller는 Entity → Response 변환을 하지 않습니다.**
- UseCase에서 이미 PaymentResponse, List<PaymentResponse>를 반환
- Controller는 단순히 HTTP 요청/응답만 처리

### 주요 메서드

#### `processPayment(ProcessPaymentRequest request)`
```java
@PostMapping
public ResponseEntity<PaymentResponse> processPayment(@RequestBody ProcessPaymentRequest request) {
    PaymentResponse response = processPaymentUseCase.execute(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**작업 흐름:**
1. UseCase에서 결제 처리 후 PaymentResponse 직접 반환
2. HTTP 201 CREATED와 함께 Response 반환
3. **변환 로직 없음**

#### `getPayments(Long userId)`
```java
@GetMapping
public ResponseEntity<PaymentListResponse> getPayments(@RequestParam Long userId) {
    List<PaymentResponse> payments = getPaymentsUseCase.execute(userId);
    return ResponseEntity.ok(new PaymentListResponse(payments));
}
```

**작업 흐름:**
1. UseCase에서 List<PaymentResponse> 직접 반환
2. PaymentListResponse로 래핑
3. HTTP 200 OK와 함께 Response 반환

#### `cancelPayment(Long paymentId)`
```java
@PostMapping("/{paymentId}/cancel")
public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long paymentId) {
    PaymentResponse response = cancelPaymentUseCase.execute(paymentId);
    return ResponseEntity.ok(response);
}
```

**작업 흐름:**
1. UseCase에서 결제 취소 후 PaymentResponse 반환
2. HTTP 200 OK와 함께 Response 반환

---

## 설계 결정 및 트레이드오프

### 아키텍처 패턴 혼재
현재 프로젝트에는 **두 가지 패턴이 혼재**되어 있습니다:

#### 패턴 1: Controller에서 변환 (User, Cart, Product, Coupon)
```
UseCase → Entity 반환 → Controller에서 Entity → Response 변환
```

#### 패턴 2: UseCase에서 변환 (Order, Payment)
```
UseCase → Response 반환 → Controller에서 그대로 반환
```

### 패턴 1 (Controller 변환)의 장점
1. **레이어 간 책임 명확화**
   - UseCase: 도메인 로직만 담당
   - Controller: Presentation 로직만 담당

2. **UseCase 재사용성 향상**
   - UseCase는 Presentation 계층에 독립적
   - 다양한 API (REST, GraphQL 등)에서 재사용 가능

3. **테스트 용이성**
   - UseCase 테스트 시 Response 객체 생성 불필요
   - 도메인 로직에만 집중한 테스트 가능

### 패턴 1 (Controller 변환)의 단점
1. **Controller 복잡도 증가**
   - 변환 로직이 Controller로 이동
   - 코드량 증가

2. **CartController의 Repository 의존성**
   - Controller가 Repository를 직접 의존
   - 계층 간 의존성 규칙 위반 가능성

3. **변환 로직 중복**
   - toLocalDateTime 같은 공통 로직이 여러 Controller에 중복

### 패턴 2 (UseCase 변환)의 장점
1. **Controller 단순화**
   - Controller는 HTTP 처리만 담당
   - 코드가 간결함

2. **일관된 응답 형식**
   - UseCase에서 응답 형식 통제
   - API 일관성 유지 용이

### 패턴 2 (UseCase 변환)의 단점
1. **UseCase의 Presentation 계층 의존**
   - UseCase가 Response 객체를 알고 있어야 함
   - 레이어 간 의존성 방향 위반

2. **UseCase 재사용성 감소**
   - 다른 API 형식(GraphQL 등)에서 재사용 어려움
   - API 형식 변경 시 UseCase도 수정 필요

3. **테스트 복잡도 증가**
   - UseCase 테스트 시 Response 객체 생성 필요
   - 도메인 로직 외에 변환 로직도 테스트해야 함

### 개선 방향
1. **아키텍처 패턴 통일**
   - 현재 두 가지 패턴이 혼재되어 있어 일관성 부족
   - 팀 내 논의를 통해 하나의 패턴으로 통일 필요
   - 권장: 패턴 1 (Controller 변환) - 레이어 간 의존성 방향 준수

2. **Mapper 계층 도입**
   - Entity → Response 변환을 담당하는 별도 Mapper 계층
   - Controller의 복잡도 감소
   - 변환 로직 재사용 및 테스트 용이

3. **CartController 리팩토링**
   - Cart + Product + Option 조합을 담당하는 별도 UseCase 생성
   - Controller의 Repository 의존성 제거
   - 예: `GetCartWithDetailsUseCase` 생성

4. **공통 유틸리티 추출**
   - toLocalDateTime 등을 공통 유틸리티 클래스로 추출
   - 코드 중복 제거
   - 예: `TimeConverter` 또는 `DateTimeUtil` 클래스

5. **Order/Payment UseCase 리팩토링 (선택사항)**
   - OrderEntity, PaymentEntity 반환으로 변경
   - Controller에서 변환 로직 추가
   - 패턴 통일 및 레이어 간 의존성 개선

---

## 요약

현재 프로젝트는 **두 가지 아키텍처 패턴이 혼재**되어 있습니다:

1. **User, Cart, Product, Coupon**: Controller에서 Entity → Response 변환
2. **Order, Payment**: UseCase에서 Response 직접 반환

각 패턴마다 장단점이 있으나, 레이어 간 의존성 방향과 재사용성을 고려하면 **패턴 1 (Controller 변환)로 통일**하는 것을 권장합니다. 추가로 Mapper 계층 도입과 공통 유틸리티 추출을 통해 Controller의 복잡도를 줄이고 코드 중복을 제거할 수 있습니다.
