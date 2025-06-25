package com.example.ptpt.repository;

import com.example.ptpt.entity.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedRepository extends JpaRepository<Feed, Long> {
    Page<Feed> findAll(Pageable pageable);

    Page<Feed> findByUserIdIn(List<Long> userIds, Pageable pageable);

    // 나 제외한 피드 (팔로우 건이 없을 때)
    Page<Feed> findByUserIdNot(Long userId, Pageable pageable);

    Page<Feed> findByUserIdNotIn(List<Long> userIds, Pageable pageable);
}