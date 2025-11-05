package com.hhplus.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse {
    private Integer totalElements;
    private Integer totalPages;
    private Integer size;
    private Integer number;
}
