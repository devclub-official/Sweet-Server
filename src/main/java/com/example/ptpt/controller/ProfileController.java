package com.example.ptpt.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "프로필", description = "프로필 관련 API")
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Operation(summary = "프로필 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(createMockProfile(userId));
    }

    @Operation(summary = "프로필 수정")
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> request) {
        Map<String, Object> profile = createMockProfile(1L);
        profile.put("username", request.get("username"));
        profile.put("bio", request.get("bio"));
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile() {
        return ResponseEntity.ok(createMockProfile(1L));
    }

    @Operation(summary = "내 포스팅 목록 조회")
    @GetMapping("/me/posts")
    public ResponseEntity<Map<String, Object>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> posts = Arrays.asList(
            createMockPost(1L),
            createMockPost(2L)
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", posts);
        response.put("totalPages", 1);
        
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createMockProfile(Long userId) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", userId);
        profile.put("username", "user" + userId);
        profile.put("profileImage", "profile.jpg");
        profile.put("bio", "운동 열심히 하는 중");
        profile.put("followersCount", 100);
        profile.put("followingCount", 50);
        profile.put("postsCount", 30);
        profile.put("isFollowing", true);
        return profile;
    }

    private Map<String, Object> createMockPost(Long id) {
        Map<String, Object> post = new HashMap<>();
        post.put("id", id);
        post.put("title", "포스트 " + id);
        post.put("content", "내용 " + id);
        post.put("imageUrls", Arrays.asList("image1.jpg", "image2.jpg"));
        return post;
    }
} 