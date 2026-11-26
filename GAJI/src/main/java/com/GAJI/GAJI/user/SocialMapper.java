package com.GAJI.GAJI.user;

import com.GAJI.GAJI.user.dto.UserSummary;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 프로필 · 팔로우 · 비밀번호 변경용 쿼리 (순번 11~16). */
@Mapper
public interface SocialMapper {

    @Select("SELECT EXISTS (SELECT 1 FROM `user` WHERE login_id = #{loginId})")
    boolean existsUser(@Param("loginId") String loginId);

    // 팔로우·언팔로우에서 두 회원의 카운트를 고치기 전에 행을 잠근다. 없는 회원이면 null.
    @Select("SELECT login_id FROM `user` WHERE login_id = #{loginId} FOR UPDATE")
    String lockUser(@Param("loginId") String loginId);

    // 프로필 조회. viewerId가 null(비로그인)이면 followedByMe는 false.
    @Select("""
            SELECT u.login_id AS loginId, u.name, u.bio, l.name AS locationName,
                   u.user_rank AS userRank, u.follower_count AS followerCount,
                   u.following_count AS followingCount,
                   EXISTS (
                       SELECT 1 FROM follow f
                       WHERE f.s_id = #{viewerId,jdbcType=VARCHAR} AND f.r_id = u.login_id
                   ) AS followedByMe
            FROM `user` u
            JOIN location l ON l.location_id = u.location_id
            WHERE u.login_id = #{loginId}
            """)
    @ConstructorArgs({
            @Arg(column = "loginId", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "bio", javaType = String.class),
            @Arg(column = "locationName", javaType = String.class),
            @Arg(column = "userRank", javaType = int.class),
            @Arg(column = "followerCount", javaType = int.class),
            @Arg(column = "followingCount", javaType = int.class),
            @Arg(column = "followedByMe", javaType = boolean.class)
    })
    ProfileRow findProfile(@Param("loginId") String loginId, @Param("viewerId") String viewerId);

    // 이미 팔로우했으면 기본키 중복으로 DuplicateKeyException
    @Insert("INSERT INTO follow (s_id, r_id) VALUES (#{me}, #{target})")
    void insertFollow(@Param("me") String me, @Param("target") String target);

    @Delete("DELETE FROM follow WHERE s_id = #{me} AND r_id = #{target}")
    int deleteFollow(@Param("me") String me, @Param("target") String target);

    @Update("UPDATE `user` SET following_count = following_count + #{change} WHERE login_id = #{loginId}")
    void changeFollowingCount(@Param("loginId") String loginId, @Param("change") int change);

    @Update("UPDATE `user` SET follower_count = follower_count + #{change} WHERE login_id = #{loginId}")
    void changeFollowerCount(@Param("loginId") String loginId, @Param("change") int change);

    @Select("""
            SELECT u.login_id AS loginId, u.name
            FROM follow f
            JOIN `user` u ON u.login_id = f.s_id
            WHERE f.r_id = #{loginId}
            ORDER BY u.login_id
            """)
    @ConstructorArgs({
            @Arg(column = "loginId", javaType = String.class),
            @Arg(column = "name", javaType = String.class)
    })
    List<UserSummary> findFollowers(@Param("loginId") String loginId);

    @Select("""
            SELECT u.login_id AS loginId, u.name
            FROM follow f
            JOIN `user` u ON u.login_id = f.r_id
            WHERE f.s_id = #{loginId}
            ORDER BY u.login_id
            """)
    @ConstructorArgs({
            @Arg(column = "loginId", javaType = String.class),
            @Arg(column = "name", javaType = String.class)
    })
    List<UserSummary> findFollowing(@Param("loginId") String loginId);

    // 현재 비밀번호가 맞을 때만 변경 (대소문자까지 구분). 바뀐 행 수가 0이면 현재 비밀번호가 틀린 것.
    @Update("""
            UPDATE `user` SET password = #{newPassword}
            WHERE login_id = #{loginId} AND BINARY password = #{currentPassword}
            """)
    int updatePassword(@Param("loginId") String loginId,
                       @Param("currentPassword") String currentPassword,
                       @Param("newPassword") String newPassword);

    record ProfileRow(
            String loginId, String name, String bio, String locationName,
            int userRank, int followerCount, int followingCount, boolean followedByMe
    ) {
    }
}
