package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.response.ApiResponse;
import com.GAJI.GAJI.post.dto.PostListResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PostLikeController {

    private final PostService postService;

    public PostLikeController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/posts/{postId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LikeCount> addLike(@PathVariable("postId") int postId, HttpServletRequest request) {
        int count = postService.addLike(postId, SessionAuth.requireLoginId(request));
        return ApiResponse.created("찜했습니다.", new LikeCount(count));
    }

    @DeleteMapping("/posts/{postId}/likes")
    public ApiResponse<LikeCount> cancelLike(@PathVariable("postId") int postId, HttpServletRequest request) {
        int count = postService.cancelLike(postId, SessionAuth.requireLoginId(request));
        return ApiResponse.ok("찜을 취소했습니다.", new LikeCount(count));
    }

    @GetMapping("/users/me/likes")
    public ApiResponse<PostListResponse> findLikedPosts(
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        String userId = SessionAuth.requireLoginId(request);
        return ApiResponse.ok("찜한 게시글을 조회했습니다.", postService.findLikedPosts(userId, sort, page, size));
    }

    public record LikeCount(int likesCount) {
    }
}
