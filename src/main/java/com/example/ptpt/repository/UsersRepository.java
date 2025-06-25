package com.example.ptpt.repository;

import com.example.ptpt.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsersRepository extends JpaRepository<UserEntity,Long> {


    @Query("""
      SELECT u
        FROM UserEntity u
       WHERE u.id <> :userId
         AND u.id NOT IN (
               SELECT f.following.id
                 FROM Follows f
                WHERE f.follower.id = :userId
           )
    """)
    Page<UserEntity> findUnfollowedUsers(@Param("userId") Long userId, Pageable pageable);
}
