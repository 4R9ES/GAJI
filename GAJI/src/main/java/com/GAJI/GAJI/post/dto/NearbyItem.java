package com.GAJI.GAJI.post.dto;

/** 근처 판매글 목록의 한 항목. PostSummary에 거리(km)가 더해진 형태. */
public record NearbyItem(
        int postId,
        String sellerId,
        String sellerName,
        String content,
        String contentImg,
        int price,
        String condition,
        String dealStatus,
        int likesCount,
        double distanceKm
) {
}
