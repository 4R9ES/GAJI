package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import com.GAJI.GAJI.post.dto.NearbyItem;
import com.GAJI.GAJI.post.dto.NearbyListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NearbyService {

    private static final double MAX_RADIUS_KM = 20000; // 지구 반 바퀴. 이보다 큰 값은 의미가 없다.

    private final NearbyMapper nearbyMapper;

    public NearbyService(NearbyMapper nearbyMapper) {
        this.nearbyMapper = nearbyMapper;
    }

    @Transactional(readOnly = true)
    public NearbyListResponse findNearby(String myId, double radiusKm, boolean includeSold,
                                         String sort, int page, int size) {
        if (!(radiusKm > 0) || radiusKm > MAX_RADIUS_KM || page < 0 || size < 1) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        NearbySort nearbySort = NearbySort.from(sort);
        long offset = (long) page * size;
        List<NearbyItem> fetched = nearbyMapper.findNearby(myId, radiusKm, includeSold, nearbySort,
                offset, (long) size + 1);
        boolean hasNext = fetched.size() > size;
        List<NearbyItem> items = List.copyOf(fetched.subList(0, Math.min(size, fetched.size())));
        return new NearbyListResponse(items, page, size, hasNext);
    }
}
