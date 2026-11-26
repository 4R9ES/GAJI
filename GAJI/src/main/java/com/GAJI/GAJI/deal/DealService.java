package com.GAJI.GAJI.deal;

import com.GAJI.GAJI.deal.dto.DealCreateRequest;
import com.GAJI.GAJI.deal.dto.DealCreateResponse;
import com.GAJI.GAJI.deal.dto.DealSummary;
import com.GAJI.GAJI.deal.dto.RatingRequest;
import com.GAJI.GAJI.deal.dto.RatingResponse;
import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DealService {

    private final DealMapper dealMapper;

    public DealService(DealMapper dealMapper) {
        this.dealMapper = dealMapper;
    }

    /**
     * 27. 거래 등록. 판매자가 구매자와 금액을 지정한다. 예약(RESERVED)을 거치지 않고 ON_SALE에서 바로 등록할 수 있다.
     * 입력값 400 → 글 없음 404 → 남의 글 403 → 본인과 거래 400 → 이미 거래 완료 409 → 구매자 없음 404 순서로 확인한다.
     */
    @Transactional
    public DealCreateResponse createDeal(String sellerId, DealCreateRequest request) {
        if (request == null || request.postId() == null
                || request.buyerId() == null || request.buyerId().isBlank()
                || request.money() == null || request.money() < 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        DealMapper.PostRow post = dealMapper.lockPost(request.postId());
        if (post == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!post.sellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_NOT_SELLER);
        }
        if (request.buyerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_SELF);
        }
        if ("SOLD".equals(post.dealStatus())) {
            throw new BusinessException(ErrorCode.DEAL_ALREADY_SOLD);
        }
        if (!dealMapper.existsUser(request.buyerId())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        var deal = new DealMapper.NewDeal(request.postId(), request.buyerId(), request.money());
        try {
            dealMapper.insertDeal(deal);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.DEAL_ALREADY_SOLD);
        }
        dealMapper.markSold(request.postId());
        return new DealCreateResponse(deal.dealId);
    }

    @Transactional(readOnly = true)
    public List<DealSummary> findPurchases(String loginId) {
        return dealMapper.findPurchases(loginId);
    }

    @Transactional(readOnly = true)
    public List<DealSummary> findSales(String loginId) {
        return dealMapper.findSales(loginId);
    }

    /**
     * 30. 판매자 평가 (구매자만, 거래당 1회). 점수 범위 400 → 거래 없음 404 → 구매자 아님 403 → 이미 평가 409 순서로 확인한다.
     */
    @Transactional
    public RatingResponse rate(int dealId, String loginId, RatingRequest request) {
        if (request == null || request.score() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        int score = request.score();
        if (score < 0 || score > 10) {
            throw new BusinessException(ErrorCode.DEAL_INVALID_SCORE);
        }
        DealMapper.DealRow deal = dealMapper.lockDeal(dealId);
        if (deal == null) {
            throw new BusinessException(ErrorCode.DEAL_NOT_FOUND);
        }
        if (!deal.buyerId().equals(loginId)) {
            throw new BusinessException(ErrorCode.DEAL_NOT_BUYER);
        }
        if (deal.buyerScore() != null) {
            throw new BusinessException(ErrorCode.DEAL_ALREADY_RATED);
        }
        dealMapper.updateScore(dealId, score);
        dealMapper.updateSellerRank(deal.sellerId(), score);
        return new RatingResponse(dealMapper.findUserRank(deal.sellerId()));
    }
}
