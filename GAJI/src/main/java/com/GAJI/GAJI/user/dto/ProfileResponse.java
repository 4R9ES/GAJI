package com.GAJI.GAJI.user.dto;

/** GET /api/users/{loginId} 응답 result. 남에게 보이는 정보만 담는다 (이메일·계좌 제외). */
public record ProfileResponse(
        String loginId,
        String name,
        String bio,
        String locationName,
        int userRank,
        int followerCount,
        int followingCount,
        boolean followedByMe
) {
}
