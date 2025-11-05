package com.hhplus.ecommerce.presentation.user.res;

import com.hhplus.ecommerce.common.dto.PageResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PointHistoryResponse extends PageResponse {
    private List<PointHistoryItemResponse> content;

    public PointHistoryResponse(List<PointHistoryItemResponse> content, Integer totalElements, Integer totalPages, Integer size, Integer number) {
        super(totalElements, totalPages, size, number);
        this.content = content;
    }
}
