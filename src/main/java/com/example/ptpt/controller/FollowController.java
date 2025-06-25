package com.example.ptpt.controller;

import com.example.ptpt.dto.response.FollowStatusResponse;
import com.example.ptpt.dto.response.UserResponse;
import com.example.ptpt.dto.response.FollowSuggestionResponse;
import com.example.ptpt.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@Tag(name = "팔로우", description = "팔로우 관련 API")
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "팔로우 하기")
    @PostMapping
    public ResponseEntity<Map<String, String>> follow(
            @Parameter(description = "팔로우 요청자 사용자 ID", example = "1", required = true)
            @RequestParam Long followerId,

            @Parameter(description = "팔로우 대상 사용자 ID", example = "2", required = true)
            @RequestParam Long targetUserId
    ) {
        followService.follow(followerId, targetUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully followed"));
    }

    @Operation(summary = "언팔로우 하기")
    @DeleteMapping
    public ResponseEntity<Map<String, String>> unfollow(
            @Parameter(description = "언팔로우 요청자 사용자 ID", example = "1", required = true)
            @RequestParam Long followerId,

            @Parameter(description = "언팔로우 대상 사용자 ID", example = "2", required = true)
            @RequestParam Long targetUserId
    ) {
        followService.unfollow(followerId, targetUserId);
        return ResponseEntity.ok(Map.of("message", "Successfully unfollowed"));
    }

    @Operation(summary = "팔로워 목록 조회", description =
            "페이징 응답(Page) 객체의 필수 정보:\n" +
                    "- **content**: 현재 페이지의 데이터 목록\n" +
                    "- **number**: 현재 페이지 번호 (0부터 시작)\n" +
                    "- **size**: 한 페이지당 항목 수\n" +
                    "- **totalElements**: 전체 데이터 개수\n" +
                    "- **totalPages**: 전체 페이지 수\n" +
                    "- **first**: 첫 페이지 여부\n" +
                    "- **last**: 마지막 페이지 여부")
    @GetMapping("/followers")
    public ResponseEntity<Page<UserResponse>> getFollowers(
            @Parameter(description = "조회할 대상 사용자 ID", example = "2", required = true)
            @RequestParam Long userId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> followers = followService.getFollowers(userId, pageRequest);
        return ResponseEntity.ok(followers);
    }

    @Operation(summary = "팔로잉 목록 조회", description =
            "페이징 응답(Page) 객체의 필수 정보:\n" +
                    "- **content**: 현재 페이지의 데이터 목록\n" +
                    "- **number**: 현재 페이지 번호 (0부터 시작)\n" +
                    "- **size**: 한 페이지당 항목 수\n" +
                    "- **totalElements**: 전체 데이터 개수\n" +
                    "- **totalPages**: 전체 페이지 수\n" +
                    "- **first**: 첫 페이지 여부\n" +
                    "- **last**: 마지막 페이지 여부")
    @GetMapping("/following")
    public ResponseEntity<Page<UserResponse>> getFollowing(
            @Parameter(description = "조회할 사용자 ID", example = "1", required = true)
            @RequestParam Long userId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> following = followService.getFollowing(userId, pageRequest);
        return ResponseEntity.ok(following);
    }

    @Operation(summary = "팔로우 상태 확인",
            description = "followerId가 targetUserId를 팔로우 중인지 여부를 반환합니다.")
    @GetMapping("/status")
    public ResponseEntity<FollowStatusResponse> isFollowing(
            @Parameter(description = "조회하는 팔로우 요청자 ID", example = "1", required = true)
            @RequestParam Long followerId,

            @Parameter(description = "조회 대상 사용자 ID", example = "2", required = true)
            @RequestParam Long targetUserId
    ) {
        boolean following = followService.isFollowing(followerId, targetUserId);
        FollowStatusResponse dto = FollowStatusResponse.builder()
                .following(following)
                .build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "추천 팔로우 대상 사용자 목록 조회", description = "현재 사용자가 팔로우하지 않은 사용자 목록을 페이징 조회합니다.")
    @GetMapping("/suggestions")
    public ResponseEntity<Page<FollowSuggestionResponse>> getSuggestions(
            @Parameter(description = "조회하는 사용자 ID", example = "1", required = true)
            @RequestParam Long userId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("nickname").ascending());
        Page<FollowSuggestionResponse> suggestions = followService.getUnfollowedUsers(userId, pageable);
        return ResponseEntity.ok(suggestions);
    }
}