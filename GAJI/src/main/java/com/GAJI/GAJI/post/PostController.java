package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.response.ApiResponse;
import com.GAJI.GAJI.post.dto.PostCreateResponse;
import com.GAJI.GAJI.post.dto.PostDetailResponse;
import com.GAJI.GAJI.post.dto.PostListResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostCreateResponse> createPost(
            HttpServletRequest request,
            @RequestParam(name = "content", required = false) String content,
            @RequestParam(name = "price", required = false) String price,
            @RequestParam(name = "condition", required = false) String condition,
            @RequestParam(name = "image", required = false) List<MultipartFile> images) {
        String sellerId = SessionAuth.requireLoginId(request);
        return ApiResponse.created("판매글을 등록했습니다.",
                postService.createPost(sellerId, content, price, condition, images));
    }

    @GetMapping
    public ApiResponse<PostListResponse> findPosts(
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok("게시글 목록을 조회했습니다.", postService.findPosts(userId, sort, page, size));
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> findPost(@PathVariable("postId") int postId, HttpServletRequest request) {
        String viewerId = SessionAuth.findLoginId(request);
        return ApiResponse.ok("게시글을 조회했습니다.", postService.findPost(postId, viewerId));
    }
}
