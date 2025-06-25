package com.example.ptpt.service.impl;

import com.example.ptpt.dto.request.CommentRequest;
import com.example.ptpt.dto.request.FeedRequest;
import com.example.ptpt.dto.response.CommentResponse;
import com.example.ptpt.dto.response.FeedDetailResponse;
import com.example.ptpt.dto.response.FeedLikeResponse;
import com.example.ptpt.dto.response.FeedResponse;
import com.example.ptpt.entity.*;
import com.example.ptpt.enums.FeedType;
import com.example.ptpt.enums.FeedVisibility;
import com.example.ptpt.repository.*;
import com.example.ptpt.service.FeedService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final FeedRepository feedRepository;
    private final UsersRepository usersRepository;
    private final FeedImagesRepository feedImagesRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FollowsRepository followRepository;
    private final CommentRepository commentRepository;

    // application.yml 에 설정된 원본 경로 (예: "file:img/feed")
    // 업로드 디렉터리 계산 시 사용
    @Value("${ptpt.upload.imagePath}")
    private String rawImagePath;

    // initImageDir() 에서 계산된 실제 업로드 경로 저장
    private Path imageDir;

    // 클라이언트가 이미지 조회할 때 앞에 붙는 URL 경로 접두어
    @Value("${ptpt.upload.urlPrefix:/feeds/images/}")
    private String imageUrlPrefix;

    // 클라이언트가 프로필 이미지 조회할 때 앞에 붙는 URL 경로 접두어
    @Value("${ptpt.upload.profileUrlPrefix:/profiles/images/}")
    private String profileUrlPrefix;

    @PostConstruct
    public void initImageDir() throws IOException {
        String sub = rawImagePath.startsWith("file:")
                ? rawImagePath.substring("file:".length())
                : rawImagePath;  // => "img/feed"

        String projectRoot = System.getProperty("user.dir");
        Path rootPath = Paths.get(projectRoot);
        imageDir = rootPath.resolve(sub).toAbsolutePath().normalize();
        Files.createDirectories(imageDir);
    }

    @Override
    public Page<FeedResponse> getFeeds(Pageable pageable, FeedType type, Long currentUserId) {
        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(currentUserId);

        Page<Feed> feedPage;
        switch (type) {
            case FOLLOWING -> {
                // 팔로우 중인 유저가 없으면 빈 페이지 반환
                if (followingIds.isEmpty()) {
                    return Page.empty(pageable);
                }
                feedPage = feedRepository.findByUserIdIn(followingIds, pageable);
            }
            case UNFOLLOWED -> {
                if (followingIds.isEmpty()) {
                    // 나 자신만 제외
                    feedPage = feedRepository.findByUserIdNot(currentUserId, pageable);
                } else {
                    List<Long> exclude = new ArrayList<>(followingIds);
                    exclude.add(currentUserId);
                    feedPage = feedRepository.findByUserIdNotIn(exclude, pageable);
                }
            }
            default -> {
                // 타입이 없거나 다른 경우 전체 피드
                feedPage = feedRepository.findAll(pageable);
            }
        }

        return feedPage.map(f -> convertToDto(f, currentUserId));
    }
    @Override
    public FeedDetailResponse getFeedById(Long id,Long currentUserId) {
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feed not found: " + id));

        List<FeedImages> feedImages = feedImagesRepository.findByFeed(feed);
        List<String> imageUrls = feedImages.stream()
                .map(fi -> imageUrlPrefix + fi.getImageUrl())
                .collect(Collectors.toList());

        boolean isLiked = usersRepository.findById(currentUserId)
                .map(user -> feedLikeRepository.existsByFeedAndUser(feed, user))
                .orElse(false);

        return convertToDetailDto(feed, imageUrls, isLiked);
    }

    @Override
    public FeedResponse createFeed(FeedRequest feedRequest, Long currentUserId) {
        UserEntity user = usersRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "User not found: " + currentUserId));
        Feed feed = new Feed();
        feed.setTitle(feedRequest.getTitle());
        feed.setContent(feedRequest.getContent());
        feed.setImage(feedRequest.getImage());
        feed.setExerciseType(feedRequest.getExerciseType());
        feed.setExerciseTime(feedRequest.getExerciseTime());
        feed.setWorkoutDuration(feedRequest.getWorkoutDuration());
        feed.setUser(user);
        feed.setVisibility(
                Optional.ofNullable(feedRequest.getVisibility())
                        .orElse(FeedVisibility.PUBLIC)
        );

        Feed saved = feedRepository.save(feed);

        return convertToDto(saved, currentUserId);
    }

    @Override
    public FeedResponse updateFeed(Long id, FeedRequest feedRequest, Long currentUserId) {
        Feed feed = feedRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Feed not found: " + id));

        if (!feed.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to update this feed");
        }

        feed.setTitle(feedRequest.getTitle());
        feed.setContent(feedRequest.getContent());
        feed.setImage(feedRequest.getImage());
        feed.setExerciseType(feedRequest.getExerciseType());
        feed.setExerciseTime(feedRequest.getExerciseTime());
        feed.setWorkoutDuration(feedRequest.getWorkoutDuration());
        feed.setVisibility(feedRequest.getVisibility());

        Feed saved = feedRepository.save(feed);
        return convertToDto(saved, currentUserId);
    }

    @Override
    public void deleteFeed(Long feedId, Long currentUserId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feed not found: "+feedId));
        if (!feed.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다");
        }
        feedRepository.delete(feed);
    }

    private File getUploadDir() {
        return imageDir.toFile();
    }

    private void deletePhysicalFile(FeedImages feedImages) {
        String fileName = feedImages.getImageUrl();
        if (fileName != null) {
            File uploadDir = getUploadDir();
            File fileToDelete = new File(uploadDir, fileName);
            if (fileToDelete.exists() && fileToDelete.delete()) {
                log.info("Deleted file: {}", fileToDelete.getAbsolutePath());
            } else {
                log.warn("Failed to delete file: {}", fileToDelete.getAbsolutePath());
            }
        }
    }

    @Override
    public List<String> uploadImages(Long feedId, List<MultipartFile> files) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new RuntimeException("Feed not found with id: " + feedId));

        List<String> imageUrls = new ArrayList<>();
        File uploadDir = getUploadDir();

        for (MultipartFile file : files) {
            String original = file.getOriginalFilename();
            if (original == null || original.isEmpty()) continue;

            File dest = new File(uploadDir, original);
            try {
                file.transferTo(dest);

                feedImagesRepository.save(FeedImages.builder()
                        .feed(feed)
                        .imageUrl(original)
                        .build());

                imageUrls.add(imageUrlPrefix + original);

            } catch (IOException e) {
                throw new RuntimeException("파일 업로드 실패: " + original, e);
            }
        }
        return imageUrls;
    }

    private FeedDetailResponse convertToDetailDto(Feed feed, List<String> imageUrls,  boolean isLikedByCurrentUser) {
        long likeCount = feedLikeRepository.countByFeed(feed);

        Optional<FeedLikes> first = feedLikeRepository.findFirstByFeedOrderByCreatedAtAsc(feed);
        String firstUser = first.map(fl -> fl.getUser().getNickname()).orElse("");
        String firstProfile = first
                .map(fl -> profileUrlPrefix + fl.getUser().getProfileImage())
                .orElse("");

        List<CommentResponse> comments = commentRepository
                .findByFeedOrderByCreatedAtAsc(feed, Pageable.ofSize(3))
                .stream()
                .map(c -> CommentResponse.builder()
                        .commentId(c.getId())
                        .userId(c.getUser().getId())
                        .userName(c.getUser().getNickname())
                        .profileImageUrl(profileUrlPrefix + c.getUser().getProfileImage())
                        .text(c.getText())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FeedDetailResponse.builder()
                .id(feed.getId())
                .title(feed.getTitle())
                .authorId(feed.getUser().getId())
                .authorName(feed.getUser().getNickname())
                .authorProfileImageUrl(profileUrlPrefix + feed.getUser().getProfileImage())
                .imageUrls(imageUrls)
                .exerciseType(feed.getExerciseType())
                .exerciseTime(feed.getExerciseTime())
                .workoutDuration(feed.getWorkoutDuration())
                .feedContent(feed.getContent())
                .visibility(feed.getVisibility())
                .likeCount(likeCount)
                .isLikedByCurrentUser(isLikedByCurrentUser)  // 여기 반영
                .firstLikedUserName(firstUser)
                .firstLikedUserProfileImageUrl(firstProfile)
                .commentCount(commentRepository.countByFeedId(feed.getId()))
                .topComments(comments)
                .shareCount(3L)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }


    private FeedResponse convertToDto(Feed feed, Long currentUserId) {
        long likeCount = feedLikeRepository.countByFeed(feed);

        boolean isLiked = usersRepository.findById(currentUserId)
                .map(user -> feedLikeRepository.existsByFeedAndUser(feed, user))
                .orElse(false);

        Optional<FeedLikes> first = feedLikeRepository.findFirstByFeedOrderByCreatedAtAsc(feed);
        String firstUser = first.map(fl -> fl.getUser().getNickname()).orElse("");
        String firstProfile = first
                .map(fl -> profileUrlPrefix + fl.getUser().getProfileImage())
                .orElse("");

        List<String> images = feedImagesRepository.findByFeed(feed)
                .stream()
                .map(fi -> imageUrlPrefix + fi.getImageUrl())
                .collect(Collectors.toList());

        return FeedResponse.builder()
                .id(feed.getId())
                .title(feed.getTitle())
                .authorId(feed.getUser().getId())
                .authorName(feed.getUser().getNickname())
                .authorProfileImageUrl(profileUrlPrefix + feed.getUser().getProfileImage())
                .imageUrls(images)
                .feedContent(feed.getContent())
                .visibility(feed.getVisibility())
                .exerciseType(feed.getExerciseType())
                .exerciseTime(feed.getExerciseTime())
                .workoutDuration(feed.getWorkoutDuration())
                .likeCount(likeCount)
                .isLikedByCurrentUser(isLiked)
                .firstLikedUserName(firstUser)
                .firstLikedUserProfileImageUrl(firstProfile)
                .commentCount(commentRepository.countByFeedId(feed.getId()))
                .shareCount(3L)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }

    @Override
    public void likeFeed(Long feedId, Long userId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new EntityNotFoundException("Feed not found: " + feedId));
        UserEntity user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (feedLikeRepository.existsByFeedAndUser(feed, user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Like already exists");
        }

        FeedLikes like = new FeedLikes();
        like.setFeed(feed);
        like.setUser(user);
        feedLikeRepository.save(like);
    }

    @Override
    @Transactional
    public void unlikeFeed(Long feedId, Long userId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new EntityNotFoundException("Feed not found: " + feedId));
        UserEntity user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (!feedLikeRepository.existsByFeedAndUser(feed, user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Like does not exist");
        }

        feedLikeRepository.deleteByFeedIdAndUserId(feedId, userId);
    }

    @Override
    public List<FeedLikeResponse> getFeedLikes(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feed not found: " + feedId));

        return feedLikeRepository.findByFeedOrderByCreatedAtDesc(feed)
                .stream()
                .map(fl -> new FeedLikeResponse(
                        fl.getUser().getId(),
                        fl.getUser().getNickname(),
                        profileUrlPrefix + fl.getUser().getProfileImage(),
                        fl.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public Page<CommentResponse> getComments(Long feedId, Pageable pageable) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Feed not found: " + feedId));

        return commentRepository.findByFeedOrderByCreatedAtAsc(feed, pageable)
                .map(c -> CommentResponse.builder()
                        .commentId(c.getId())
                        .userId(c.getUser().getId())
                        .userName(c.getUser().getNickname())
                        .profileImageUrl(profileUrlPrefix + c.getUser().getProfileImage())
                        .text(c.getText())
                        .createdAt(c.getCreatedAt())
                        .build()
                );
    }

    @Override
    public CommentResponse createComment(Long feedId, Long userId, CommentRequest request) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feed not found: " + feedId));
        UserEntity user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        Comment comment = Comment.builder()
                .feed(feed)
                .user(user)
                .text(request.getText())
                .build();
        Comment saved = commentRepository.save(comment);

        return CommentResponse.builder()
                .commentId(saved.getId())
                .userId(user.getId())
                .userName(user.getNickname())
                .profileImageUrl(profileUrlPrefix + user.getProfileImage())
                .text(saved.getText())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId));
        comment.setText(request.getText());
        Comment updated = commentRepository.save(comment);

        return CommentResponse.builder()
                .commentId(updated.getId())
                .userId(updated.getUser().getId())
                .userName(updated.getUser().getNickname())
                .profileImageUrl(profileUrlPrefix + updated.getUser().getProfileImage())
                .text(updated.getText())
                .createdAt(updated.getCreatedAt())
                .build();
    }

    @Override
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found: " + commentId);
        }
        commentRepository.deleteById(commentId);
    }
}
