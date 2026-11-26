package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.response.ApiResponse;
import com.GAJI.GAJI.post.dto.NearbyListResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 19번: 내 동네 반경 n km 판매글. /api/posts/{postId}보다 literal 경로(nearby)가 먼저 매칭된다. */
@RestController
@RequestMapping("/api/posts")
public class NearbyController {

    private final NearbyService nearbyService;

    public NearbyController(NearbyService nearbyService) {
        this.nearbyService = nearbyService;
    }

    @GetMapping("/nearby")
    public ApiResponse<NearbyListResponse> findNearby(
            @RequestParam(name = "radiusKm", defaultValue = "5") double radiusKm,
            @RequestParam(name = "includeSold", defaultValue = "false") boolean includeSold,
            @RequestParam(name = "sort", defaultValue = "distance") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        String myId = SessionAuth.requireLoginId(request);
        return ApiResponse.ok("주변 판매글을 조회했습니다.",
                nearbyService.findNearby(myId, radiusKm, includeSold, sort, page, size));
    }
}
