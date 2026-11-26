package com.GAJI.GAJI.feed;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import com.GAJI.GAJI.post.PostSort;
import com.GAJI.GAJI.post.dto.PostListResponse;
import com.GAJI.GAJI.post.dto.PostSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FeedService {

    private final FeedMapper feedMapper;

    public FeedService(FeedMapper feedMapper) {
        this.feedMapper = feedMapper;
    }

    @Transactional(readOnly = true)
    public PostListResponse findFeed(String myId, String sort, int page, int size) {
        if (page < 0 || size < 1) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        PostSort postSort = PostSort.from(sort);
        long offset = (long) page * size; // long으로 계산해 큰 페이지 번호에서도 넘치지 않게 한다
        List<PostSummary> fetched = feedMapper.findFeed(myId, postSort, offset, (long) size + 1);
        boolean hasNext = fetched.size() > size;
        List<PostSummary> items = List.copyOf(fetched.subList(0, Math.min(size, fetched.size())));
        return new PostListResponse(items, page, size, hasNext);
    }
}
