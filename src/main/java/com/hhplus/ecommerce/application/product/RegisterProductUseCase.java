package com.hhplus.ecommerce.application.product;

import com.hhplus.ecommerce.common.exception.UserNotFoundException;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.product.ProductRepository;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterProductUseCase {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductEntity execute(Long createdUserId, String productName, String content, Long price) {
        UserEntity.validateUserId(createdUserId);

        UserEntity createdUser = userRepository.findById(createdUserId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        ProductEntity product = ProductEntity.create(createdUser, productName, content, price);
        return productRepository.save(product);
    }
}
