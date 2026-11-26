package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.response.ApiResponse;
import com.GAJI.GAJI.post.dto.PostDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 17, 18번: 판매글 수정·삭제 (본인만). */
@RestController
@RequestMapping("/api/posts")
public class PostEditController {

    private final PostEditService postEditService;
    private final PostService postService;

    public PostEditController(PostEditService postEditService, PostService postService) {
        this.postEditService = postEditService;
        this.postService = postService;
    }

    @PatchMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostDetailResponse> updatePost(
            @PathVariable("postId") int postId,
            @RequestParam(name = "content", required = false) String content,
            @RequestParam(name = "price", required = false) String price,
            @RequestParam(name = "condition", required = false) String condition,
            @RequestParam(name = "dealStatus", required = false) String dealStatus,
            @RequestParam(name = "image", required = false) List<MultipartFile> images,
            HttpServletRequest request) {
        String loginId = SessionAuth.requireLoginId(request);
        postEditService.update(postId, loginId, content, price, condition, dealStatus, images);
        return ApiResponse.ok("판매글을 수정했습니다.", postService.findPost(postId, loginId));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable("postId") int postId, HttpServletRequest request) {
        String loginId = SessionAuth.requireLoginId(request);
        postEditService.delete(postId, loginId);
        return ApiResponse.ok("판매글을 삭제했습니다.", null);
    }
}
