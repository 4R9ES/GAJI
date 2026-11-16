package com.GAJI.GAJI.global.response;

/**
 * 모든 API 응답의 공통 껍데기. 명세서의 "응답 형식"과 같다.
 * { "isSuccess": true, "code": "COMMON200", "message": "...", "result": ... }
 *
 * record의 구성요소 이름이 그대로 JSON 이름이 된다 (isSuccess).
 */
public record ApiResponse<T>(boolean isSuccess, String code, String message, T result) {

    /** 조회·수정·삭제 성공 (HTTP 200). 돌려줄 값이 없으면 result에 null. */
    public static <T> ApiResponse<T> ok(String message, T result) {
        return new ApiResponse<>(true, "COMMON200", message, result);
    }

    /** 생성 성공 (COMMON201). 컨트롤러 메서드에 @ResponseStatus(HttpStatus.CREATED)를 함께 붙인다. */
    public static <T> ApiResponse<T> created(String message, T result) {
        return new ApiResponse<>(true, "COMMON201", message, result);
    }

    /** 실패. GlobalExceptionHandler가 만든다. 컨트롤러에서 직접 쓸 일은 없다. */
    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }
}
