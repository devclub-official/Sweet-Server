package com.example.ptpt.repository;

import com.example.ptpt.entity.Follows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowsRepository extends JpaRepository<Follows, Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    void deleteByFollower_IdAndFollowing_Id(Long followerId, Long followingId);
    Page<Follows> findByFollowingId(Long followingId, Pageable pageable);
    Page<Follows> findByFollowerId(Long followerId, Pageable pageable);

    // JPQL: f.following.id, f.follower.id 로 접근
    @Query("SELECT f.following.id FROM Follows f WHERE f.follower.id = :followerId")
    List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);
}