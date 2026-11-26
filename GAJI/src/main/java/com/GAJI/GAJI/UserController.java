package com.GAJI.GAJI.user;

import com.GAJI.GAJI.global.response.ApiResponse;
import com.GAJI.GAJI.user.dto.SignupRequest;
import com.GAJI.GAJI.user.dto.SignupResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 3. 회원가입 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signup(@RequestBody SignupRequest request) {
        return ApiResponse.created("회원가입이 완료되었습니다.", userService.signup(request));
    }
}
