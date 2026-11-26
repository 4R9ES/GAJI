package com.GAJI.GAJI.feed;

import com.GAJI.GAJI.post.PostSort;
import com.GAJI.GAJI.post.dto.PostSummary;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 피드: 내가 팔로우한 회원들의 판매글 (순번 26). */
@Mapper
public interface FeedMapper {

    // 정렬은 고정 SQL 중에서만 고른다 (요청 값을 SQL에 직접 넣지 않는다). 같은 값이면 id가 큰 글이 먼저.
    @Select("""
            <script>
            SELECT p.id AS postId, p.user_id AS sellerId, u.name AS sellerName,
                   p.content, p.content_img AS contentImg, p.price,
                   p.`condition` AS postCondition, p.deal_status AS dealStatus,
                   p.likes_count AS likesCount
            FROM post p
            JOIN `user` u ON u.login_id = p.user_id
            WHERE EXISTS (SELECT 1 FROM follow f WHERE f.s_id = #{myId} AND f.r_id = p.user_id)
            ORDER BY
            <choose>
                <when test="sort.name() == 'PRICE_ASC'">p.price ASC, p.id DESC</when>
                <when test="sort.name() == 'PRICE_DESC'">p.price DESC, p.id DESC</when>
                <when test="sort.name() == 'LIKES'">p.likes_count DESC, p.id DESC</when>
                <otherwise>p.id DESC</otherwise>
            </choose>
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    @ConstructorArgs({
            @Arg(column = "postId", javaType = int.class),
            @Arg(column = "sellerId", javaType = String.class),
            @Arg(column = "sellerName", javaType = String.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "contentImg", javaType = String.class),
            @Arg(column = "price", javaType = int.class),
            @Arg(column = "postCondition", javaType = String.class),
            @Arg(column = "dealStatus", javaType = String.class),
            @Arg(column = "likesCount", javaType = int.class)
    })
    List<PostSummary> findFeed(@Param("myId") String myId,
                               @Param("sort") PostSort sort,
                               @Param("offset") long offset,
                               @Param("limit") long limit);
}
