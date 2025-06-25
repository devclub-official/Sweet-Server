package com.example.ptpt.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "추천 팔로우 대상 사용자 응답 DTO")
public class FollowSuggestionResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "/users/images/profile.png")
    private String profileImage;
}
