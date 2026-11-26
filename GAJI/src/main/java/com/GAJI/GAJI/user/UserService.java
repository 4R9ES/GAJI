package com.GAJI.GAJI.user;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import com.GAJI.GAJI.user.dto.LoginRequest;
import com.GAJI.GAJI.user.dto.MeResponse;
import com.GAJI.GAJI.user.dto.SignupRequest;
import com.GAJI.GAJI.user.dto.SignupResponse;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public SignupResponse signup(SignupRequest request) {
        // 입력값 검사 (길이는 schema.sql 컬럼 길이와 같다). 실패하면 COMMON400
        if (request == null
                || !required(request.loginId(), 20)
                || !required(request.password(), 255)
                || !required(request.name(), 50)
                || !required(request.email(), 255) || !EMAIL.matcher(request.email()).matches()
                || (request.bio() != null && length(request.bio()) > 300)
                || request.locationId() == null
                || !required(request.account(), 30)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (userMapper.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.USER_LOGIN_ID_DUPLICATE);
        }
        if (userMapper.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
        }
        if (!userMapper.existsLocation(request.locationId())) {
            throw new BusinessException(ErrorCode.USER_LOCATION_NOT_FOUND);
        }
        userMapper.insertUser(request.loginId(), request.password(), request.name(), request.email(),
                request.bio(), request.locationId(), request.account());
        return new SignupResponse(request.loginId());
    }

    public MeResponse login(LoginRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        // 이메일이 없든 비밀번호가 틀리든 같은 에러 (어느 쪽이 틀렸는지 알려주지 않음)
        UserMapper.MeRow row = userMapper.findByEmailAndPassword(request.email(), request.password());
        if (row == null) {
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }
        return toResponse(row);
    }

    public MeResponse me(String loginId) {
        UserMapper.MeRow row = userMapper.findByLoginId(loginId);
        if (row == null) {
            // 세션은 있지만 회원이 없는 경우 (탈퇴 등) → 다시 로그인하도록 COMMON401
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED);
        }
        return toResponse(row);
    }

    private MeResponse toResponse(UserMapper.MeRow row) {
        return new MeResponse(row.loginId(), row.name(), row.email(), row.bio(),
                new MeResponse.Location(row.locationId(), row.locationName()),
                row.account(), row.userRank(), row.followerCount(), row.followingCount());
    }

    private static boolean required(String value, int maxLength) {
        return !isBlank(value) && length(value) <= maxLength;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static int length(String value) {
        return value.codePointCount(0, value.length());
    }
}
