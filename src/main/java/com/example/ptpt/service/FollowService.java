package com.example.ptpt.service;

import com.example.ptpt.dto.response.UserResponse;
import com.example.ptpt.dto.response.FollowSuggestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowService {
    void follow(Long followerId, Long targetUserId);
    void unfollow(Long followerId, Long targetUserId);
    Page<UserResponse> getFollowers(Long userId, Pageable pageable);
    Page<UserResponse> getFollowing(Long userId, Pageable pageable);

    boolean isFollowing(Long followerId, Long targetUserId);
    Page<FollowSuggestionResponse> getUnfollowedUsers(Long currentUserId, Pageable pageable);
}