package com.GAJI.GAJI.user.dto;

/** POST /api/users 요청 본문. bio만 선택 항목이다. */
public record SignupRequest(
        String loginId,
        String password,
        String name,
        String email,
        String bio,
        Integer locationId,
        String account
) {
}
