package com.GAJI.GAJI.post.dto;

public record PostDetailResponse(
        int postId,
        Seller seller,
        String content,
        String contentImg,
        int price,
        String condition,
        String dealStatus,
        int likesCount,
        boolean likedByMe,
        boolean mine
) {
    public record Seller(String loginId, String name, int userRank, String locationName) {
    }
}
