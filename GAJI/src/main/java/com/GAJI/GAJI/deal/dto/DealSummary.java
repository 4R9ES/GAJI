package com.GAJI.GAJI.deal.dto;

/** 구매·판매 내역의 한 항목. buyerScore는 평가 전이면 null. dealDate는 "2026-11-05T15:00:00" 형태 문자열. */
public record DealSummary(
        int dealId,
        int postId,
        String content,
        String sellerId,
        String buyerId,
        int money,
        String dealDate,
        Integer buyerScore
) {
}
