package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;

public enum PostSort {
    LATEST("latest"),
    PRICE_ASC("priceAsc"),
    PRICE_DESC("priceDesc"),
    LIKES("likes");

    private final String queryValue;

    PostSort(String queryValue) {
        this.queryValue = queryValue;
    }

    public static PostSort from(String value) {
        for (PostSort sort : values()) {
            if (sort.queryValue.equals(value)) {
                return sort;
            }
        }
        throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
    }
}
