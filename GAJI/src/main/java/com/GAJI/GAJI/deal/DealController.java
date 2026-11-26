package com.GAJI.GAJI.deal;

import com.GAJI.GAJI.deal.dto.DealCreateRequest;
import com.GAJI.GAJI.deal.dto.DealCreateResponse;
import com.GAJI.GAJI.deal.dto.DealSummary;
import com.GAJI.GAJI.deal.dto.RatingRequest;
import com.GAJI.GAJI.deal.dto.RatingResponse;
import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 27~30번: 거래 등록, 구매 내역, 판매 내역, 판매자 평가. */
@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    /** 27. 거래 등록 (판매자가 구매자·금액 지정) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DealCreateResponse> createDeal(@RequestBody DealCreateRequest body, HttpServletRequest request) {
        String sellerId = SessionAuth.requireLoginId(request);
        return ApiResponse.created("거래를 등록했습니다.", dealService.createDeal(sellerId, body));
    }

    /** 28. 내 구매 내역 */
    @GetMapping("/purchases")
    public ApiResponse<List<DealSummary>> findPurchases(HttpServletRequest request) {
        String loginId = SessionAuth.requireLoginId(request);
        return ApiResponse.ok("구매 내역을 조회했습니다.", dealService.findPurchases(loginId));
    }

    /** 29. 내 판매 내역 */
    @GetMapping("/sales")
    public ApiResponse<List<DealSummary>> findSales(HttpServletRequest request) {
        String loginId = SessionAuth.requireLoginId(request);
        return ApiResponse.ok("판매 내역을 조회했습니다.", dealService.findSales(loginId));
    }

    /** 30. 판매자 평가 (구매자, 거래당 1회) */
    @PostMapping("/{dealId}/rating")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RatingResponse> rate(@PathVariable("dealId") int dealId,
                                            @RequestBody RatingRequest body,
                                            HttpServletRequest request) {
        String loginId = SessionAuth.requireLoginId(request);
        return ApiResponse.created("평가를 등록했습니다.", dealService.rate(dealId, loginId, body));
    }
}
