package com.GAJI.GAJI.user.dto;

/** POST /api/auth/login 요청 본문. 로그인은 이메일 + 비밀번호로 한다. */
public record LoginRequest(String email, String password) {
}
