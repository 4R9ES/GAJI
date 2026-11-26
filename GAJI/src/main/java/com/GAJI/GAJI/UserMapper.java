package com.GAJI.GAJI.user;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    // 회원가입: 아이디 중복 확인 (true면 USER409_1)
    @Select("SELECT EXISTS (SELECT 1 FROM `user` WHERE login_id = #{loginId})")
    boolean existsByLoginId(@Param("loginId") String loginId);

    // 회원가입: 이메일 중복 확인 (true면 USER409_2)
    @Select("SELECT EXISTS (SELECT 1 FROM `user` WHERE email = #{email})")
    boolean existsByEmail(@Param("email") String email);

    // 회원가입: 동네 존재 확인 (false면 USER400_1)
    @Select("SELECT EXISTS (SELECT 1 FROM location WHERE location_id = #{locationId})")
    boolean existsLocation(@Param("locationId") int locationId);

    // 회원가입
    @Insert("""
            INSERT INTO `user` (login_id, password, name, email, bio, location_id, account)
            VALUES (#{loginId}, #{password}, #{name}, #{email}, #{bio,jdbcType=VARCHAR}, #{locationId}, #{account})
            """)
    void insertUser(@Param("loginId") String loginId,
                    @Param("password") String password,
                    @Param("name") String name,
                    @Param("email") String email,
                    @Param("bio") String bio,
                    @Param("locationId") int locationId,
                    @Param("account") String account);

    // 로그인 (이메일 + 비밀번호가 맞으면 1행)
    @Select("""
            SELECT u.login_id AS loginId, u.name, u.email, u.bio,
                   u.location_id AS locationId, l.name AS locationName, u.account,
                   u.user_rank AS userRank, u.follower_count AS followerCount,
                   u.following_count AS followingCount
            FROM `user` u
            JOIN location l ON l.location_id = u.location_id
            WHERE u.email = #{email} AND u.password = #{password}
            """)
    @ConstructorArgs({
            @Arg(column = "loginId", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "email", javaType = String.class),
            @Arg(column = "bio", javaType = String.class),
            @Arg(column = "locationId", javaType = int.class),
            @Arg(column = "locationName", javaType = String.class),
            @Arg(column = "account", javaType = String.class),
            @Arg(column = "userRank", javaType = int.class),
            @Arg(column = "followerCount", javaType = int.class),
            @Arg(column = "followingCount", javaType = int.class)
    })
    MeRow findByEmailAndPassword(@Param("email") String email, @Param("password") String password);

    // 내 프로필 정보
    @Select("""
            SELECT u.login_id AS loginId, u.name, u.email, u.bio,
                   u.location_id AS locationId, l.name AS locationName, u.account,
                   u.user_rank AS userRank, u.follower_count AS followerCount,
                   u.following_count AS followingCount
            FROM `user` u
            JOIN location l ON l.location_id = u.location_id
            WHERE u.login_id = #{loginId}
            """)
    @ConstructorArgs({
            @Arg(column = "loginId", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "email", javaType = String.class),
            @Arg(column = "bio", javaType = String.class),
            @Arg(column = "locationId", javaType = int.class),
            @Arg(column = "locationName", javaType = String.class),
            @Arg(column = "account", javaType = String.class),
            @Arg(column = "userRank", javaType = int.class),
            @Arg(column = "followerCount", javaType = int.class),
            @Arg(column = "followingCount", javaType = int.class)
    })
    MeRow findByLoginId(@Param("loginId") String loginId);

    record MeRow(
            String loginId, String name, String email, String bio,
            int locationId, String locationName, String account,
            int userRank, int followerCount, int followingCount
    ) {
    }
}
