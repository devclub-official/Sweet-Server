package com.example.ptpt.service.impl;

import com.example.ptpt.dto.response.UserResponse;
import com.example.ptpt.dto.response.FollowSuggestionResponse;
import com.example.ptpt.entity.Follows;
import com.example.ptpt.entity.UserEntity;
import com.example.ptpt.repository.FollowsRepository;
import com.example.ptpt.repository.UsersRepository;
import com.example.ptpt.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private final FollowsRepository followRepo;
    private final UsersRepository userRepo;

    @Value("${ptpt.upload.profileUrlPrefix}")
    private String profileUrlPrefix;

    @Override
    public void follow(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }
        if (followRepo.existsByFollowerIdAndFollowingId(followerId, targetUserId)) {
            return;
        }
        UserEntity follower = userRepo.findById(followerId)
                .orElseThrow(() -> new NoSuchElementException("팔로워를 찾을 수 없습니다."));
        UserEntity target   = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("팔로우 대상을 찾을 수 없습니다."));
        Follows follow = new Follows();
        follow.setFollower(follower);
        follow.setFollowing(target);
        followRepo.save(follow);
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long targetUserId) {
        followRepo.deleteByFollower_IdAndFollowing_Id(followerId, targetUserId);
    }

    @Override
    public Page<UserResponse> getFollowers(Long userId, Pageable pageable) {
        return followRepo.findByFollowingId(userId, pageable)
                .map(f -> {
                    UserEntity u = f.getFollower();
                    // prefix가 슬래시로 끝나지 않으면 붙여 주고, 실제 파일명 앞에 붙이기
                    String prefix = profileUrlPrefix.endsWith("/")
                            ? profileUrlPrefix
                            : profileUrlPrefix + "/";
                    String fullUrl = prefix + u.getProfileImage();

                    return UserResponse.builder()
                            .id(u.getId())
                            .nickname(u.getNickname())         // ← getUsername() → getNickname()
                            .profileImageUrl(fullUrl)
                            .build();
                });
    }


    @Override
    public Page<UserResponse> getFollowing(Long userId, Pageable pageable) {
        return followRepo.findByFollowerId(userId, pageable)
                .map(f -> {
                    UserEntity u = f.getFollowing();
                    // prefix가 슬래시로 끝나지 않으면 붙여 주고, 실제 파일명 앞에 붙이기
                    String prefix = profileUrlPrefix.endsWith("/")
                            ? profileUrlPrefix
                            : profileUrlPrefix + "/";
                    String fullUrl = prefix + u.getProfileImage();

                    return UserResponse.builder()
                            .id(u.getId())
                            .nickname(u.getNickname())      // ← getUsername() → getNickname()
                            .profileImageUrl(fullUrl)
                            .build();
                });
    }

    @Override
    public boolean isFollowing(Long followerId, Long targetUserId) {
        return followRepo.existsByFollowerIdAndFollowingId(followerId, targetUserId);
    }

    @Override
    public Page<FollowSuggestionResponse> getUnfollowedUsers(Long currentUserId, Pageable pageable) {
        return userRepo
                .findUnfollowedUsers(currentUserId, pageable)
                .map(u -> new FollowSuggestionResponse(
                        u.getId(),
                        u.getNickname(),
                        u.getProfileImage()
                ));
    }
}
