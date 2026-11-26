package com.GAJI.GAJI.comment;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("""
            INSERT INTO comment (post_id, user_id, c_content)
            VALUES (#{postId}, #{userId}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "commentId", keyColumn = "id")
    void insertComment(NewComment comment);

    @Select("""
            SELECT c.id AS commentId, c.user_id AS userId, u.name AS userName, c.c_content AS content,
                   COALESCE(c.user_id = #{viewerId,jdbcType=VARCHAR}, FALSE) AS mine
            FROM comment c
            JOIN `user` u ON u.login_id = c.user_id
            WHERE c.post_id = #{postId}
            ORDER BY c.id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "commentId", javaType = int.class),
            @Arg(column = "userId", javaType = String.class),
            @Arg(column = "userName", javaType = String.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "mine", javaType = boolean.class)
    })
    List<CommentSummary> findComments(@Param("postId") int postId, @Param("viewerId") String viewerId);

    @Select("SELECT user_id FROM comment WHERE id = #{commentId} FOR UPDATE")
    String lockCommentAuthor(@Param("commentId") int commentId);

    @Delete("DELETE FROM comment WHERE id = #{commentId}")
    void deleteComment(@Param("commentId") int commentId);

    final class NewComment {
        Integer commentId;
        final int postId;
        final String userId;
        final String content;

        NewComment(int postId, String userId, String content) {
            this.postId = postId;
            this.userId = userId;
            this.content = content;
        }
    }
}
