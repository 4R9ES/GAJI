package com.GAJI.GAJI.global.auth;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 세션 로그인 도우미. 로그인한 회원의 loginId를 세션에 저장해 두고 꺼내 쓴다.
 *
 * 컨트롤러에서 사용법:
 *   String me = SessionAuth.requireLoginId(request);   // 로그인 필요한 API. 아니면 COMMON401
 *   String me = SessionAuth.findLoginId(request);      // 로그인 안 해도 되는 API에서 "로그인했다면 누구" 확인용 (없으면 null)
 *
 * 로그인 API:  SessionAuth.login(request, loginId);
 * 로그아웃 API: SessionAuth.logout(request);
 */
public final class SessionAuth {

    private static final String LOGIN_ID_KEY = "LOGIN_ID";

    private SessionAuth() {
    }

    public static void login(HttpServletRequest request, String loginId) {
        if (request.getSession(false) != null) {
            request.changeSessionId(); // 세션 고정 공격 방지: 로그인 시 세션 ID를 새로 발급
        }
        request.getSession(true).setAttribute(LOGIN_ID_KEY, loginId);
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** 로그인 상태면 loginId, 아니면 null */
    public static String findLoginId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(LOGIN_ID_KEY);
    }

    /** 로그인 필수. 로그인 상태가 아니면 COMMON401 */
    public static String requireLoginId(HttpServletRequest request) {
        String loginId = findLoginId(request);
        if (loginId == null) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED);
        }
        return loginId;
    }
}
