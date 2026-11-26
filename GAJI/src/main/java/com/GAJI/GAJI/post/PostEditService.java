package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.exception.BusinessException;
import com.GAJI.GAJI.global.response.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Service
public class PostEditService {

    private static final Set<String> CONDITIONS = Set.of("NEW", "LIKE_NEW", "USED");
    private static final Set<String> EDITABLE_STATUS = Set.of("ON_SALE", "RESERVED");

    private final PostEditMapper postEditMapper;
    private final PostImageStorage imageStorage;

    public PostEditService(PostEditMapper postEditMapper, PostImageStorage imageStorage) {
        this.postEditMapper = postEditMapper;
        this.imageStorage = imageStorage;
    }

    /** 17. 판매글 수정. 보낸 필드만 바꾼다. 입력값 400 → 없음 404 → 남의 글 403 → 거래 완료 409 순서로 확인한다. */
    @Transactional
    public void update(int postId, String loginId, String content, String price, String condition,
                       String dealStatus, List<MultipartFile> images) {
        List<MultipartFile> files = images == null ? List.of() : images.stream().filter(f -> !f.isEmpty()).toList();

        if (content == null && price == null && condition == null && dealStatus == null && files.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (content != null && (content.isBlank() || content.codePointCount(0, content.length()) > 1000)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (condition != null && !CONDITIONS.contains(condition)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (files.size() > 1) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        Integer amount = null;
        if (price != null) {
            try {
                amount = Integer.parseInt(price.trim());
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
            }
            if (amount < 0) {
                throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
            }
        }
        if (dealStatus != null && !EDITABLE_STATUS.contains(dealStatus)) {
            throw new BusinessException(ErrorCode.POST_INVALID_STATUS);
        }

        PostEditMapper.PostRow row = postEditMapper.lockPost(postId);
        if (row == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!row.ownerId().equals(loginId)) {
            throw new BusinessException(ErrorCode.POST_FORBIDDEN);
        }
        if ("SOLD".equals(row.dealStatus())) {
            throw new BusinessException(ErrorCode.POST_SOLD_CANNOT_EDIT);
        }

        Path image = files.isEmpty() ? null : imageStorage.save(files.getFirst());
        try {
            postEditMapper.updatePost(postId, content, amount, condition,
                    image == null ? null : "/uploads/" + image.getFileName(), dealStatus);
        } catch (RuntimeException e) {
            imageStorage.deleteAfterFailure(image, e);
            throw e;
        }
    }

    /** 18. 판매글 삭제. 남의 글이면 403, 거래 기록이 있으면 409. */
    @Transactional
    public void delete(int postId, String loginId) {
        PostEditMapper.PostRow row = postEditMapper.lockPost(postId);
        if (row == null) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!row.ownerId().equals(loginId)) {
            throw new BusinessException(ErrorCode.POST_FORBIDDEN);
        }
        if (postEditMapper.hasDeal(postId)) {
            throw new BusinessException(ErrorCode.POST_HAS_DEAL_CANNOT_DELETE);
        }
        try {
            postEditMapper.deletePost(postId);
        } catch (DataIntegrityViolationException e) {
            // 확인 직후 거래가 등록돼 DB가 삭제를 막은 경우
            throw new BusinessException(ErrorCode.POST_HAS_DEAL_CANNOT_DELETE);
        }
    }
}
