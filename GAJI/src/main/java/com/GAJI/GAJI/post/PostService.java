package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import com.GAJI.GAJI.post.dto.PostCreateResponse;
import com.GAJI.GAJI.post.dto.PostDetailResponse;
import com.GAJI.GAJI.post.dto.PostListResponse;
import com.GAJI.GAJI.post.dto.PostSummary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Service
public class PostService {

    private static final Set<String> CONDITIONS = Set.of("NEW", "LIKE_NEW", "USED");

    private final PostMapper postMapper;
    private final PostImageStorage imageStorage;

    public PostService(PostMapper postMapper, PostImageStorage imageStorage) {
        this.postMapper = postMapper;
        this.imageStorage = imageStorage;
    }

    public PostCreateResponse createPost(String sellerId, String content, String price,
                                         String condition, List<MultipartFile> images) {
        if (content == null || content.isBlank() || content.codePointCount(0, content.length()) > 1000
                || condition == null || !CONDITIONS.contains(condition)
                || (images != null && images.size() > 1)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        int amount;
        try {
            amount = Integer.parseInt(price);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (amount < 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }

        Path image = images == null || images.isEmpty() ? null : imageStorage.save(images.getFirst());
        var post = new PostMapper.NewPost(sellerId, content, amount, condition,
                image == null ? null : "/uploads/" + image.getFileName());
        try {
            postMapper.insertPost(post);
        } catch (RuntimeException e) {
            imageStorage.deleteAfterFailure(image, e);
            throw e;
        }
        return new PostCreateResponse(post.postId);
    }

    public PostListResponse findPosts(String userId, String sort, int page, int size) {
        return findPosts(userId, null, sort, page, size);
    }

    public PostListResponse findLikedPosts(String userId, String sort, int page, int size) {
        return findPosts(null, userId, sort, page, size);
    }

    private PostListResponse findPosts(String userId, String likedBy, String sort, int page, int size) {
        if (page < 0 || size < 1) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        PostSort postSort = PostSort.from(sort);
        // long으로 계산해 큰 페이지 번호에서도 int 오버플로가 발생하지 않게 한다.
        long offset = (long) page * size;
        List<PostSummary> fetched = postMapper.findPosts(userId, likedBy, postSort, offset, (long) size + 1);
        boolean hasNext = fetched.size() > size;
        List<PostSummary> items = List.copyOf(fetched.subList(0, Math.min(size, fetched.size())));
        return new PostListResponse(items, page, size, hasNext);
    }

    @Transactional
    public int addLike(int postId, String userId) {
        // 같은 게시글의 찜 추가와 취소는 카운터를 갱신하기 전에 게시글 행을 먼저 잠근다.
        if (postMapper.lockPost(postId) == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        try {
            postMapper.insertLike(userId, postId);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.LIKE_ALREADY);
        }
        postMapper.changeLikesCount(postId, 1);
        return postMapper.findLikesCount(postId);
    }

    @Transactional
    public int cancelLike(int postId, String userId) {
        if (postMapper.lockPost(postId) == null || postMapper.deleteLike(userId, postId) == 0) {
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }
        postMapper.changeLikesCount(postId, -1);
        return postMapper.findLikesCount(postId);
    }

    public PostDetailResponse findPost(int postId, String viewerId) {
        PostMapper.PostDetailRow post = postMapper.findPost(postId, viewerId);
        if (post == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        var seller = new PostDetailResponse.Seller(post.sellerId(), post.sellerName(),
                post.userRank(), post.locationName());
        return new PostDetailResponse(post.postId(), seller, post.content(), post.contentImg(),
                post.price(), post.condition(), post.dealStatus(), post.likesCount(),
                post.likedByMe(), post.mine());
    }
}
