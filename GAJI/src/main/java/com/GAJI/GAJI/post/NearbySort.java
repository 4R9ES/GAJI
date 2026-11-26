package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;

/** 근처 검색 정렬. 허용 목록에 없는 값은 COMMON400. (SQL에는 이 값을 직접 넣지 않고 고정 SQL 중에서만 고른다) */
public enum NearbySort {
    DISTANCE("distance"),
    LATEST("latest"),
    PRICE_ASC("priceAsc"),
    PRICE_DESC("priceDesc"),
    LIKES("likes");

    private final String queryValue;

    NearbySort(String queryValue) {
        this.queryValue = queryValue;
    }

    public static NearbySort from(String value) {
        for (NearbySort sort : values()) {
            if (sort.queryValue.equals(value)) {
                return sort;
            }
        }
        throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
    }
}
