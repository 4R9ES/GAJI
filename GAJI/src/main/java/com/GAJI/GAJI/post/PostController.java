package com.GAJI.GAJI.post;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostMapper postMapper;

    @GetMapping("/stats")
    public List<Map<String, Object>> stats() {
        return postMapper.tableCounts();
    }

    @GetMapping("/posts")
    public List<Map<String, Object>> posts() {
        return postMapper.findAllPosts();
    }

    @GetMapping("/posts/likes")
    public List<Map<String, Object>> likeCounts() {
        return postMapper.postLikeCounts();
    }

    @GetMapping("/posts/{postId}/comments")
    public List<Map<String, Object>> comments(@PathVariable int postId) {
        return postMapper.findComments(postId);
    }

    @GetMapping("/users/{userId}/liked-posts")
    public List<Map<String, Object>> likedPosts(@PathVariable int userId) {
        return postMapper.findLikedPosts(userId);
    }
}
