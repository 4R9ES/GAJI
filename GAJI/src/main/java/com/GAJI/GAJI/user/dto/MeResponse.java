package com.GAJI.GAJI.user.dto;

/** 로그인(POST /api/auth/login)과 내 정보(GET /api/auth/me)의 응답 result. */
public record MeResponse(
        String loginId,
        String name,
        String email,
        String bio,
        Location location,
        String account,
        int userRank,
        int followerCount,
        int followingCount
) {
    public record Location(int locationId, String name) {
    }
}
