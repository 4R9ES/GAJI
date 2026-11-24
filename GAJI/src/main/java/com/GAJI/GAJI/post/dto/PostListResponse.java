package com.GAJI.GAJI.post.dto;

import java.util.List;

public record PostListResponse(List<PostSummary> items, int page, int size, boolean hasNext) {
}
