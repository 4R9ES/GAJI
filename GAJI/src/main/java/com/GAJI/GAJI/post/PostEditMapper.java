package com.GAJI.GAJI.post;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 판매글 수정·삭제용 쿼리 (순번 17, 18). */
@Mapper
public interface PostEditMapper {

    // 수정·삭제 중에 상태가 바뀌지 않도록 게시글 행을 잠그고 작성자와 거래 상태를 읽는다. 없으면 null.
    @Select("SELECT user_id AS ownerId, deal_status AS dealStatus FROM post WHERE id = #{postId} FOR UPDATE")
    @ConstructorArgs({
            @Arg(column = "ownerId", javaType = String.class),
            @Arg(column = "dealStatus", javaType = String.class)
    })
    PostRow lockPost(@Param("postId") int postId);

    // 보낸 필드만 바꾼다 (null인 필드는 건드리지 않는다).
    @Update("""
            <script>
            UPDATE post
            <set>
                <if test="content != null">content = #{content},</if>
                <if test="price != null">price = #{price},</if>
                <if test="condition != null">`condition` = #{condition},</if>
                <if test="contentImg != null">content_img = #{contentImg},</if>
                <if test="dealStatus != null">deal_status = #{dealStatus},</if>
            </set>
            WHERE id = #{postId}
            </script>
            """)
    void updatePost(@Param("postId") int postId,
                    @Param("content") String content,
                    @Param("price") Integer price,
                    @Param("condition") String condition,
                    @Param("contentImg") String contentImg,
                    @Param("dealStatus") String dealStatus);

    @Select("SELECT EXISTS (SELECT 1 FROM deal_log WHERE post_id = #{postId})")
    boolean hasDeal(@Param("postId") int postId);

    // 댓글·찜은 ON DELETE CASCADE로 같이 지워진다.
    @Delete("DELETE FROM post WHERE id = #{postId}")
    void deletePost(@Param("postId") int postId);

    record PostRow(String ownerId, String dealStatus) {
    }
}
