package com.GAJI.GAJI.user;

import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.response.ApiResponse;
import com.GAJI.GAJI.user.dto.PasswordChangeRequest;
import com.GAJI.GAJI.user.dto.ProfileResponse;
import com.GAJI.GAJI.user.dto.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 11~16번: 프로필, 팔로우, 언팔로우, 팔로워·팔로잉 목록, 비밀번호 변경. */
@RestController
@RequestMapping("/api/users")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    /** 11. 프로필 조회 (로그인 안 해도 됨. 로그인했다면 followedByMe 계산) */
    @GetMapping("/{loginId}")
    public ApiResponse<ProfileResponse> findProfile(@PathVariable("loginId") String loginId,
                                                    HttpServletRequest request) {
        String viewerId = SessionAuth.findLoginId(request);
        return ApiResponse.ok("프로필을 조회했습니다.", socialService.findProfile(loginId, viewerId));
    }

    /** 12. 팔로우 */
    @PostMapping("/{loginId}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> follow(@PathVariable("loginId") String loginId, HttpServletRequest request) {
        String me = SessionAuth.requireLoginId(request);
        socialService.follow(me, loginId);
        return ApiResponse.created("팔로우했습니다.", null);
    }

    /** 13. 언팔로우 */
    @DeleteMapping("/{loginId}/follow")
    public ApiResponse<Void> unfollow(@PathVariable("loginId") String loginId, HttpServletRequest request) {
        String me = SessionAuth.requireLoginId(request);
        socialService.unfollow(me, loginId);
        return ApiResponse.ok("팔로우를 취소했습니다.", null);
    }

    /** 14. 팔로워 목록 */
    @GetMapping("/{loginId}/followers")
    public ApiResponse<List<UserSummary>> findFollowers(@PathVariable("loginId") String loginId) {
        return ApiResponse.ok("팔로워 목록을 조회했습니다.", socialService.findFollowers(loginId));
    }

    /** 15. 팔로잉 목록 */
    @GetMapping("/{loginId}/following")
    public ApiResponse<List<UserSummary>> findFollowing(@PathVariable("loginId") String loginId) {
        return ApiResponse.ok("팔로잉 목록을 조회했습니다.", socialService.findFollowing(loginId));
    }

    /** 16. 비밀번호 변경 */
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(@RequestBody PasswordChangeRequest body, HttpServletRequest request) {
        String me = SessionAuth.requireLoginId(request);
        socialService.changePassword(me, body);
        return ApiResponse.ok("비밀번호를 변경했습니다.", null);
    }
}
