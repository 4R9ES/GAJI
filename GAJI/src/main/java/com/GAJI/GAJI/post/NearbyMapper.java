package com.GAJI.GAJI.post;

import com.GAJI.GAJI.post.dto.NearbyItem;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 내 동네 반경 n km 판매글 (순번 19). */
@Mapper
public interface NearbyMapper {

    // 거리 = 내 동네 좌표와 판매자 동네 좌표 사이 (km, 소수 첫째 자리).
    // 판매 완료(SOLD) 글은 includeSold가 true일 때만 포함한다.
    // 정렬은 고정 SQL 중에서만 고르고, 같은 값이면 id가 큰 글이 먼저 오게 해서 페이지가 안정적이다.
    @Select("""
            <script>
            SELECT p.id AS postId, p.user_id AS sellerId, u.name AS sellerName,
                   p.content, p.content_img AS contentImg, p.price,
                   p.`condition` AS postCondition, p.deal_status AS dealStatus,
                   p.likes_count AS likesCount,
                   ROUND(ST_Distance_Sphere(POINT(sl.lng, sl.lat), POINT(ml.lng, ml.lat)) / 1000, 1) AS distanceKm
            FROM post p
            JOIN `user` u ON u.login_id = p.user_id
            JOIN location sl ON sl.location_id = u.location_id
            JOIN `user` me ON me.login_id = #{myId}
            JOIN location ml ON ml.location_id = me.location_id
            WHERE (#{includeSold} = TRUE OR p.deal_status &lt;&gt; 'SOLD')
            HAVING distanceKm &lt;= #{radiusKm}
            ORDER BY
            <choose>
                <when test="sort.name() == 'LATEST'">p.id DESC</when>
                <when test="sort.name() == 'PRICE_ASC'">p.price ASC, p.id DESC</when>
                <when test="sort.name() == 'PRICE_DESC'">p.price DESC, p.id DESC</when>
                <when test="sort.name() == 'LIKES'">p.likes_count DESC, p.id DESC</when>
                <otherwise>distanceKm ASC, p.id DESC</otherwise>
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
            @Arg(column = "likesCount", javaType = int.class),
            @Arg(column = "distanceKm", javaType = double.class)
    })
    List<NearbyItem> findNearby(@Param("myId") String myId,
                                @Param("radiusKm") double radiusKm,
                                @Param("includeSold") boolean includeSold,
                                @Param("sort") NearbySort sort,
                                @Param("offset") long offset,
                                @Param("limit") long limit);
}
