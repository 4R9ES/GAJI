package com.GAJI.GAJI.global.response;

import org.springframework.http.HttpStatus;

/**
 * 명세서 "에러 코드 전체 목록"과 같은 순서·같은 값.
 * 새 에러가 필요하면 명세서를 먼저 고치고 여기에 추가한다.
 */
public enum ErrorCode {

    // 공통
    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "입력값이 올바르지 않습니다."),
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "로그인이 필요합니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청한 주소를 찾을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405", "지원하지 않는 요청 방식입니다."),
    COMMON_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 오류가 발생했습니다."),

    // 회원
    USER_LOCATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "USER400_1", "존재하지 않는 동네입니다."),
    USER_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USER401_1", "아이디 또는 비밀번호가 올바르지 않습니다."),
    USER_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "USER401_2", "현재 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "존재하지 않는 회원입니다."),
    USER_LOGIN_ID_DUPLICATE(HttpStatus.CONFLICT, "USER409_1", "이미 사용 중인 아이디입니다."),
    USER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "USER409_2", "이미 사용 중인 이메일입니다."),

    // 팔로우
    FOLLOW_SELF(HttpStatus.BAD_REQUEST, "FOLLOW400_1", "자기 자신은 팔로우할 수 없습니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLLOW404_1", "팔로우하지 않은 회원입니다."),
    FOLLOW_ALREADY(HttpStatus.CONFLICT, "FOLLOW409_1", "이미 팔로우한 회원입니다."),

    // 게시글
    POST_INVALID_STATUS(HttpStatus.BAD_REQUEST, "POST400_1", "거래 상태는 ON_SALE 또는 RESERVED로만 바꿀 수 있습니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "POST403_1", "본인이 작성한 게시글만 수정·삭제할 수 있습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST404_1", "존재하지 않는 게시글입니다."),
    POST_SOLD_CANNOT_EDIT(HttpStatus.CONFLICT, "POST409_1", "거래가 완료된 게시글은 수정할 수 없습니다."),
    POST_HAS_DEAL_CANNOT_DELETE(HttpStatus.CONFLICT, "POST409_2", "거래 기록이 있는 게시글은 삭제할 수 없습니다."),

    // 댓글
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT403_1", "본인이 작성한 댓글만 삭제할 수 있습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT404_1", "존재하지 않는 댓글입니다."),

    // 찜
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE404_1", "찜하지 않은 게시글입니다."),
    LIKE_ALREADY(HttpStatus.CONFLICT, "LIKE409_1", "이미 찜한 게시글입니다."),

    // 거래
    DEAL_SELF(HttpStatus.BAD_REQUEST, "DEAL400_1", "본인과는 거래할 수 없습니다."),
    DEAL_INVALID_SCORE(HttpStatus.BAD_REQUEST, "DEAL400_2", "점수는 0~10 사이여야 합니다."),
    DEAL_NOT_BUYER(HttpStatus.FORBIDDEN, "DEAL403_1", "구매자만 평가할 수 있습니다."),
    DEAL_NOT_SELLER(HttpStatus.FORBIDDEN, "DEAL403_2", "본인이 올린 판매글만 거래를 등록할 수 있습니다."),
    DEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "DEAL404_1", "존재하지 않는 거래입니다."),
    DEAL_ALREADY_SOLD(HttpStatus.CONFLICT, "DEAL409_1", "이미 거래가 완료된 게시글입니다."),
    DEAL_ALREADY_RATED(HttpStatus.CONFLICT, "DEAL409_2", "이미 평가한 거래입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
