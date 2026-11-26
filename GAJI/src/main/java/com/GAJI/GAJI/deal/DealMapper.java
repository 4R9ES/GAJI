package com.GAJI.GAJI.deal;

import com.GAJI.GAJI.deal.dto.DealSummary;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 거래 등록, 구매·판매 내역, 평가 (순번 27~30). */
@Mapper
public interface DealMapper {

    // 거래 등록 중에 글 상태가 바뀌지 않도록 게시글 행을 잠그고 판매자와 거래 상태를 읽는다. 없으면 null.
    @Select("SELECT user_id AS sellerId, deal_status AS dealStatus FROM post WHERE id = #{postId} FOR UPDATE")
    @ConstructorArgs({
            @Arg(column = "sellerId", javaType = String.class),
            @Arg(column = "dealStatus", javaType = String.class)
    })
    PostRow lockPost(@Param("postId") int postId);

    @Select("SELECT EXISTS (SELECT 1 FROM `user` WHERE login_id = #{loginId})")
    boolean existsUser(@Param("loginId") String loginId);

    // post_id는 UNIQUE라서 한 글에 거래는 한 번만 들어간다 (중복이면 DuplicateKeyException).
    @Insert("""
            INSERT INTO deal_log (post_id, buyer_id, money)
            VALUES (#{postId}, #{buyerId}, #{money})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "dealId", keyColumn = "deal_id")
    void insertDeal(NewDeal deal);

    @Update("UPDATE post SET deal_status = 'SOLD' WHERE id = #{postId}")
    void markSold(@Param("postId") int postId);

    // 내 구매 내역 (최신순). 시간이 같으면 dealId가 큰 거래가 먼저.
    @Select("""
            SELECT d.deal_id AS dealId, d.post_id AS postId, p.content, p.user_id AS sellerId,
                   d.buyer_id AS buyerId, d.money,
                   DATE_FORMAT(d.deal_date, '%Y-%m-%dT%H:%i:%s') AS dealDate, d.buyer_score AS buyerScore
            FROM deal_log d
            JOIN post p ON p.id = d.post_id
            WHERE d.buyer_id = #{loginId}
            ORDER BY d.deal_date DESC, d.deal_id DESC
            """)
    @ConstructorArgs({
            @Arg(column = "dealId", javaType = int.class),
            @Arg(column = "postId", javaType = int.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "sellerId", javaType = String.class),
            @Arg(column = "buyerId", javaType = String.class),
            @Arg(column = "money", javaType = int.class),
            @Arg(column = "dealDate", javaType = String.class),
            @Arg(column = "buyerScore", javaType = Integer.class)
    })
    List<DealSummary> findPurchases(@Param("loginId") String loginId);

    // 내 판매 내역 (최신순)
    @Select("""
            SELECT d.deal_id AS dealId, d.post_id AS postId, p.content, p.user_id AS sellerId,
                   d.buyer_id AS buyerId, d.money,
                   DATE_FORMAT(d.deal_date, '%Y-%m-%dT%H:%i:%s') AS dealDate, d.buyer_score AS buyerScore
            FROM deal_log d
            JOIN post p ON p.id = d.post_id
            WHERE p.user_id = #{loginId}
            ORDER BY d.deal_date DESC, d.deal_id DESC
            """)
    @ConstructorArgs({
            @Arg(column = "dealId", javaType = int.class),
            @Arg(column = "postId", javaType = int.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "sellerId", javaType = String.class),
            @Arg(column = "buyerId", javaType = String.class),
            @Arg(column = "money", javaType = int.class),
            @Arg(column = "dealDate", javaType = String.class),
            @Arg(column = "buyerScore", javaType = Integer.class)
    })
    List<DealSummary> findSales(@Param("loginId") String loginId);

    // 평가 중에 같은 거래가 두 번 평가되지 않도록 거래 행을 잠그고 구매자, 평가 여부, 판매자를 읽는다. 없으면 null.
    @Select("""
            SELECT d.buyer_id AS buyerId, d.buyer_score AS buyerScore, p.user_id AS sellerId
            FROM deal_log d
            JOIN post p ON p.id = d.post_id
            WHERE d.deal_id = #{dealId}
            FOR UPDATE
            """)
    @ConstructorArgs({
            @Arg(column = "buyerId", javaType = String.class),
            @Arg(column = "buyerScore", javaType = Integer.class),
            @Arg(column = "sellerId", javaType = String.class)
    })
    DealRow lockDeal(@Param("dealId") int dealId);

    @Update("UPDATE deal_log SET buyer_score = #{score} WHERE deal_id = #{dealId}")
    void updateScore(@Param("dealId") int dealId, @Param("score") int score);

    // 판매자 신뢰도 += (점수 - 5), 0~100으로 자른다.
    @Update("""
            UPDATE `user` SET user_rank = LEAST(100, GREATEST(0, user_rank + #{score} - 5))
            WHERE login_id = #{sellerId}
            """)
    void updateSellerRank(@Param("sellerId") String sellerId, @Param("score") int score);

    @Select("SELECT user_rank FROM `user` WHERE login_id = #{loginId}")
    int findUserRank(@Param("loginId") String loginId);

    record PostRow(String sellerId, String dealStatus) {
    }

    record DealRow(String buyerId, Integer buyerScore, String sellerId) {
    }

    final class NewDeal {
        Integer dealId;
        final int postId;
        final String buyerId;
        final int money;

        NewDeal(int postId, String buyerId, int money) {
            this.postId = postId;
            this.buyerId = buyerId;
            this.money = money;
        }
    }
}
