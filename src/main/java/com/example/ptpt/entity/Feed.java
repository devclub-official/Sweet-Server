package com.example.ptpt.entity;

import com.example.ptpt.enums.FeedVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feeds")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 운동 이미지 URL */
    @Column(length = 500)
    private String image;

    /** 운동 종류 (러닝, 요가, ...) */
    @Column(name = "exercise_type", length = 50)
    private String exerciseType;

    /** 운동 시간대 (아침, 저녁 등) */
    @Column(name = "exercise_time", length = 50)
    private String exerciseTime;

    /** 운동 시간 (분) */
    @Column(name = "workout_duration")
    private Integer workoutDuration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedVisibility visibility;

    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP")
    private Instant updatedAt;

    @OneToOne(mappedBy = "feed", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ExerciseDetails exerciseDetails;

    @Builder.Default
    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedImages> feedImages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
