package com.product.batch.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCsvRow {

    private String productId;
    private String name;
    private String category;
    private String price;
    private String quantity;
    private String createdDate;

    public String toRawData() {
        return String.join(",",
                safe(productId),
                safe(name),
                safe(category),
                safe(price),
                safe(quantity),
                safe(createdDate));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}