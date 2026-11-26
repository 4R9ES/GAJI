package com.GAJI.GAJI.user;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import com.GAJI.GAJI.user.dto.PasswordChangeRequest;
import com.GAJI.GAJI.user.dto.ProfileResponse;
import com.GAJI.GAJI.user.dto.UserSummary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SocialService {

    private static final int PASSWORD_MAX_LENGTH = 255;

    private final SocialMapper socialMapper;

    public SocialService(SocialMapper socialMapper) {
        this.socialMapper = socialMapper;
    }

    @Transactional(readOnly = true)
    public ProfileResponse findProfile(String loginId, String viewerId) {
        SocialMapper.ProfileRow row = socialMapper.findProfile(loginId, viewerId);
        if (row == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return new ProfileResponse(row.loginId(), row.name(), row.bio(), row.locationName(),
                row.userRank(), row.followerCount(), row.followingCount(), row.followedByMe());
    }

    @Transactional
    public void follow(String me, String target) {
        if (me.equals(target)) {
            throw new BusinessException(ErrorCode.FOLLOW_SELF);
        }
        lockBoth(me, target);
        try {
            socialMapper.insertFollow(me, target);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.FOLLOW_ALREADY);
        }
        socialMapper.changeFollowingCount(me, 1);
        socialMapper.changeFollowerCount(target, 1);
    }

    @Transactional
    public void unfollow(String me, String target) {
        if (me.equals(target)) {
            throw new BusinessException(ErrorCode.FOLLOW_NOT_FOUND);
        }
        lockBoth(me, target);
        if (socialMapper.deleteFollow(me, target) == 0) {
            throw new BusinessException(ErrorCode.FOLLOW_NOT_FOUND);
        }
        socialMapper.changeFollowingCount(me, -1);
        socialMapper.changeFollowerCount(target, -1);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> findFollowers(String loginId) {
        requireUser(loginId);
        return socialMapper.findFollowers(loginId);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> findFollowing(String loginId) {
        requireUser(loginId);
        return socialMapper.findFollowing(loginId);
    }

    @Transactional
    public void changePassword(String loginId, PasswordChangeRequest request) {
        if (request == null
                || request.currentPassword() == null || request.currentPassword().isBlank()
                || request.newPassword() == null || request.newPassword().isBlank()
                || request.newPassword().codePointCount(0, request.newPassword().length()) > PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (socialMapper.updatePassword(loginId, request.currentPassword(), request.newPassword()) == 0) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_MISMATCH);
        }
    }

    private void requireUser(String loginId) {
        if (!socialMapper.existsUser(loginId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 두 회원 행을 loginId 순서로 잠근다. 서로를 동시에 팔로우해도 같은 순서로 잠그기 때문에 교착(deadlock)이 생기지 않는다.
     * 내가 없으면 COMMON401, 상대가 없으면 USER404_1.
     */
    private void lockBoth(String me, String target) {
        boolean meFirst = me.compareTo(target) <= 0;
        String first = meFirst ? me : target;
        String second = meFirst ? target : me;
        String lockedFirst = socialMapper.lockUser(first);
        String lockedSecond = socialMapper.lockUser(second);
        String lockedMe = meFirst ? lockedFirst : lockedSecond;
        String lockedTarget = meFirst ? lockedSecond : lockedFirst;
        if (lockedMe == null) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED);
        }
        if (lockedTarget == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
