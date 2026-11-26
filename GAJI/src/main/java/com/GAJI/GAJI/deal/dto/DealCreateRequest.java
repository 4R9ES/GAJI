package com.GAJI.GAJI.deal.dto;

/** POST /api/deals 요청 본문. 판매자가 구매자와 금액을 지정한다. */
public record DealCreateRequest(Integer postId, String buyerId, Integer money) {
}
