package com.GAJI.GAJI.user.dto;

/** PATCH /api/users/me/password 요청 본문. */
public record PasswordChangeRequest(String currentPassword, String newPassword) {
}
