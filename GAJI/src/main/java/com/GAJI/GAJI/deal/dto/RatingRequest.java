package com.GAJI.GAJI.deal.dto;

/** POST /api/deals/{dealId}/rating 요청 본문. score는 0~10. */
public record RatingRequest(Integer score) {
}
