package com.GAJI.GAJI.post.dto;

/** 게시글 목록, 피드, 찜 목록, 반경 검색에서 공통으로 사용하는 항목. */
public record PostSummary(
        int postId,
        String sellerId,
        String sellerName,
        String content,
        String contentImg,
        int price,
        String condition,
        String dealStatus,
        int likesCount
) {
}
