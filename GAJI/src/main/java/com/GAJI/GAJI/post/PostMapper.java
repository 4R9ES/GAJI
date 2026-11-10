package com.GAJI.GAJI.post;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper {

    @Select("""
            SELECT 'location' AS table_name, COUNT(*) AS total FROM location
            UNION ALL SELECT 'User', COUNT(*) FROM User
            UNION ALL SELECT 'Post', COUNT(*) FROM Post
            UNION ALL SELECT 'follow', COUNT(*) FROM follow
            UNION ALL SELECT 'Likes', COUNT(*) FROM Likes
            UNION ALL SELECT 'Comment', COUNT(*) FROM Comment
            UNION ALL SELECT 'Deal_log', COUNT(*) FROM Deal_log
            """)
    List<Map<String, Object>> tableCounts();

    @Select("SELECT * FROM Post")
    List<Map<String, Object>> findAllPosts();

    @Select("""
            SELECT P.ID, P.content, COUNT(L.user_ID) AS likes_count
            FROM Post P
            LEFT JOIN Likes L ON P.ID = L.post_ID
            GROUP BY P.ID, P.content
            """)
    List<Map<String, Object>> postLikeCounts();

    @Select("SELECT * FROM Comment WHERE post_ID = #{postId}")
    List<Map<String, Object>> findComments(int postId);

    @Select("""
            SELECT P.ID, P.content, P.price
            FROM Post P
            JOIN Likes L ON P.ID = L.post_ID
            WHERE L.user_ID = #{userId}
            """)
    List<Map<String, Object>> findLikedPosts(int userId);
}
