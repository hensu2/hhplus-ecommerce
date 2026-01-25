package com.hhplus.ecommerce.application.productOption;

import com.hhplus.ecommerce.common.exception.ProductOptionNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockChangedKafkaEvent;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockEventType;
import com.hhplus.ecommerce.infrastructure.kafka.producer.StockKafkaProducer;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.productOption.req.UpdateStockRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateStockUseCase {

    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;
    private final StockKafkaProducer stockKafkaProducer;

    public ProductOptionEntity execute(Long optionId, UpdateStockRequest request) {
        ProductOptionEntity option = productOptionRepository.findById(optionId)
            .orElseThrow(() -> new ProductOptionNotFoundException("옵션을 찾을 수 없습니다. ID: " + optionId));

        Long previousStock = option.getStock();
        option.updateStock(request.type(), request.amount());
        ProductOptionEntity updatedOption = productOptionRepository.save(option);

        // Kafka 이벤트 발행 (Dual Write Pattern)
        try {
            ProductEntity product = productRepository.getOrThrow(option.getProductId());
            StockEventType eventType = switch (request.type()) {
                case INCREASE -> StockEventType.STOCK_INCREASED;
                case DECREASE -> StockEventType.STOCK_DECREASED;
                case SET -> StockEventType.STOCK_UPDATED;
            };

            StockChangedKafkaEvent event = new StockChangedKafkaEvent(
                eventType,
                updatedOption,
                product.getProductName(),
                previousStock,
                updatedOption.getStock(),
                request.amount(),
                "ADMIN_UPDATE"
            );
            stockKafkaProducer.publish(event);
        } catch (Exception e) {
            // Kafka 발행 실패는 로깅만 하고 재고 처리는 계속 진행
        }

        return updatedOption;
    }
}