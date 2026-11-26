package com.GAJI.GAJI.user;

import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.response.ApiResponse;
import com.GAJI.GAJI.user.dto.LoginRequest;
import com.GAJI.GAJI.user.dto.MeResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 5. 로그인. 성공하면 세션에 loginId를 저장한다 (JSESSIONID 쿠키). */
    @PostMapping("/login")
    public ApiResponse<MeResponse> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        MeResponse me = userService.login(body);
        SessionAuth.login(request, me.loginId());
        return ApiResponse.ok("로그인했습니다.", me);
    }

    /** 6. 내 정보 조회 (로그인 상태 확인). 로그인 안 했으면 COMMON401 */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(HttpServletRequest request) {
        String loginId = SessionAuth.requireLoginId(request);
        return ApiResponse.ok("내 정보를 조회했습니다.", userService.me(loginId));
    }

    /** 7. 로그아웃. 로그인 안 했으면 COMMON401 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        SessionAuth.requireLoginId(request);
        SessionAuth.logout(request);
        return ApiResponse.ok("로그아웃했습니다.", null);
    }
}
