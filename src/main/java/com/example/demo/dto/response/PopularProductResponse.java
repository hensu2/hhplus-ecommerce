package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularProductResponse {
    private Long id;
    private String productName;
    private Integer price;
    private Integer salesCount;
    private Integer ranking;
}
