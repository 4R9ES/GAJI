package com.GAJI.GAJI.comment;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import com.GAJI.GAJI.post.PostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;

    public CommentService(CommentMapper commentMapper, PostMapper postMapper) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
    }

    @Transactional
    public int createComment(int postId, String authorId, String content) {
        if (content == null || content.isBlank() || content.codePointCount(0, content.length()) > 300) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (postMapper.lockPost(postId) == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        var comment = new CommentMapper.NewComment(postId, authorId, content);
        commentMapper.insertComment(comment);
        return comment.commentId;
    }

    @Transactional(readOnly = true)
    public List<CommentSummary> findComments(int postId, String viewerId) {
        if (!postMapper.postExists(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return commentMapper.findComments(postId, viewerId);
    }

    @Transactional
    public void deleteComment(int commentId, String authorId) {
        String existingAuthor = commentMapper.lockCommentAuthor(commentId);
        if (existingAuthor == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!existingAuthor.equals(authorId)) {
            throw new BusinessException(ErrorCode.COMMENT_FORBIDDEN);
        }
        commentMapper.deleteComment(commentId);
    }
}
