package com.hhplus.ecommerce.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PRODUCT_LIST_KEY_PREFIX = "products:page:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public Optional<Page<ProductEntity>> getProductListCache(Pageable pageable) {
        String cacheKey = buildProductListCacheKey(pageable);

        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue == null) {
                return Optional.empty();
            }

            CachedPageData cachedPage = objectMapper.readValue(cachedValue, CachedPageData.class);
            List<ProductEntity> products = objectMapper.convertValue(
                cachedPage.content,
                new TypeReference<List<ProductEntity>>() {}
            );

            Page<ProductEntity> page = new PageImpl<>(
                products,
                pageable,
                cachedPage.totalElements
            );

            log.debug("Cache hit for product list: {}", cacheKey);
            return Optional.of(page);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached product list: {}", cacheKey, e);
            return Optional.empty();
        }
    }

    public void setProductListCache(Pageable pageable, Page<ProductEntity> page) {
        String cacheKey = buildProductListCacheKey(pageable);

        try {
            CachedPageData cachedData = new CachedPageData(
                page.getContent(),
                page.getTotalElements()
            );

            String jsonValue = objectMapper.writeValueAsString(cachedData);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, CACHE_TTL);

            log.debug("Cached product list: {}", cacheKey);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize product list to cache: {}", cacheKey, e);
        }
    }

    public void evictAllProductListCache() {
        Set<String> keys = redisTemplate.keys(PRODUCT_LIST_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} product list cache entries", keys.size());
        }
    }

    private String buildProductListCacheKey(Pageable pageable) {
        return PRODUCT_LIST_KEY_PREFIX +
            pageable.getPageNumber() + ":size:" +
            pageable.getPageSize() + ":sort:" +
            pageable.getSort().toString().replace(": ", ":");
    }

    private static class CachedPageData {
        public Object content;
        public long totalElements;

        public CachedPageData() {}

        public CachedPageData(Object content, long totalElements) {
            this.content = content;
            this.totalElements = totalElements;
        }
    }
}
