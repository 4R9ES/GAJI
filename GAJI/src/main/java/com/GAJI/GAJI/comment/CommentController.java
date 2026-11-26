package com.GAJI.GAJI.comment;

import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatedComment> createComment(@PathVariable("postId") int postId,
                                                     @RequestBody(required = false) Map<String, Object> body,
                                                     HttpServletRequest request) {
        String authorId = SessionAuth.requireLoginId(request);
        Object content = body == null ? null : body.get("content");
        int commentId = commentService.createComment(postId, authorId, content instanceof String text ? text : null);
        return ApiResponse.created("댓글을 작성했습니다.", new CreatedComment(commentId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentSummary>> findComments(@PathVariable("postId") int postId,
                                                          HttpServletRequest request) {
        String viewerId = SessionAuth.findLoginId(request);
        return ApiResponse.ok("댓글 목록을 조회했습니다.", commentService.findComments(postId, viewerId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable("commentId") int commentId, HttpServletRequest request) {
        commentService.deleteComment(commentId, SessionAuth.requireLoginId(request));
        return ApiResponse.ok("댓글을 삭제했습니다.", null);
    }

    public record CreatedComment(int commentId) {
    }
}
