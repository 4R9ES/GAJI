package com.GAJI.GAJI.post;

import com.GAJI.GAJI.post.dto.PostSummary;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper {

    @Insert("""
            INSERT INTO post (user_id, content, price, `condition`, content_img)
            VALUES (#{sellerId}, #{content}, #{price}, #{condition}, #{contentImg,jdbcType=VARCHAR})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "postId", keyColumn = "id")
    void insertPost(NewPost post);

    final class NewPost {
        Integer postId;
        final String sellerId;
        final String content;
        final int price;
        final String condition;
        final String contentImg;

        NewPost(String sellerId, String content, int price, String condition, String contentImg) {
            this.sellerId = sellerId;
            this.content = content;
            this.price = price;
            this.condition = condition;
            this.contentImg = contentImg;
        }
    }

    // 정렬은 고정 SQL 중에서만 선택한다. 요청 값을 SQL 문자열에 직접 삽입하지 않는다.
    @Select("""
            <script>
            SELECT p.id AS postId, p.user_id AS sellerId, u.name AS sellerName,
                   p.content, p.content_img AS contentImg, p.price,
                   p.`condition` AS postCondition, p.deal_status AS dealStatus,
                   p.likes_count AS likesCount
            FROM post p
            JOIN `user` u ON u.login_id = p.user_id
            <if test="userId != null">
                WHERE p.user_id = #{userId}
            </if>
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
    List<PostSummary> findPosts(@Param("userId") String userId,
                                @Param("sort") PostSort sort,
                                @Param("offset") long offset,
                                @Param("limit") long limit);

    @Select("""
            SELECT p.id AS postId, u.login_id AS sellerId, u.name AS sellerName,
                   u.user_rank AS userRank, loc.name AS locationName,
                   p.content, p.content_img AS contentImg, p.price,
                   p.`condition` AS postCondition, p.deal_status AS dealStatus,
                   p.likes_count AS likesCount,
                   EXISTS (
                       SELECT 1 FROM likes l
                       WHERE l.post_id = p.id AND l.user_id = #{viewerId,jdbcType=VARCHAR}
                   ) AS likedByMe,
                   COALESCE(u.login_id = #{viewerId,jdbcType=VARCHAR}, FALSE) AS mine
            FROM post p
            JOIN `user` u ON u.login_id = p.user_id
            JOIN location loc ON loc.location_id = u.location_id
            WHERE p.id = #{postId}
            """)
    @ConstructorArgs({
            @Arg(column = "postId", javaType = int.class),
            @Arg(column = "sellerId", javaType = String.class),
            @Arg(column = "sellerName", javaType = String.class),
            @Arg(column = "userRank", javaType = int.class),
            @Arg(column = "locationName", javaType = String.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "contentImg", javaType = String.class),
            @Arg(column = "price", javaType = int.class),
            @Arg(column = "postCondition", javaType = String.class),
            @Arg(column = "dealStatus", javaType = String.class),
            @Arg(column = "likesCount", javaType = int.class),
            @Arg(column = "likedByMe", javaType = boolean.class),
            @Arg(column = "mine", javaType = boolean.class)
    })
    PostDetailRow findPost(@Param("postId") int postId, @Param("viewerId") String viewerId);

    record PostDetailRow(
            int postId, String sellerId, String sellerName, int userRank, String locationName,
            String content, String contentImg, int price, String condition, String dealStatus,
            int likesCount, boolean likedByMe, boolean mine
    ) {
    }
}
