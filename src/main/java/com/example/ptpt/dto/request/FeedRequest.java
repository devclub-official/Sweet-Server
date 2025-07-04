package com.example.ptpt.dto.request;

import com.example.ptpt.dto.ExerciseDetailsRequest;
import com.example.ptpt.enums.FeedVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "피드 데이터 요청 객체")
public class FeedRequest {

    @Schema(description = "피드 제목", example = "오늘의 운동 일지", defaultValue = "기본 제목")
    private String title;

    @Schema(description = "피드 내용", example = "오늘 30분 운동했습니다. 기분이 좋네요.", defaultValue = "기본 내용")
    private String content;

    @Schema(description = "작성자 ID", example = "1", defaultValue = "1")
    private Long authorId;

    @Schema(description = "피드 공개 범위", example = "공개", defaultValue = "공개")
    private FeedVisibility visibility;

    // ↓ 추가된 운동 관련 필드들
    @Schema(description = "운동 이미지 URL", example = "/feeds/images/run1.jpg")
    private String image;

    @Schema(description = "운동 종류", example = "러닝")
    private String exerciseType;

    @Schema(description = "운동 시간대", example = "아침")
    private String exerciseTime;

    @Schema(description = "운동 시간(분)", example = "30")
    private Integer workoutDuration;
}
