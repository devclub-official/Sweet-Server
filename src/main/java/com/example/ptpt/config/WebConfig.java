package com.example.ptpt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 정적 리소스 URL 패턴과 실제 파일 시스템 경로를
 * 프로젝트 루트(user.dir) 기준으로 매핑합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ptpt.upload.urlPrefix}")
    private String feedUrlPrefix;

    @Value("${ptpt.upload.imagePath}")
    private String feedImagePath;

    @Value("${ptpt.upload.profileUrlPrefix}")
    private String profileUrlPrefix;

    @Value("${ptpt.upload.profileImagePath}")
    private String profileImagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 피드 이미지
        registry.addResourceHandler(toPattern(feedUrlPrefix))
                .addResourceLocations(resolveLocation(feedImagePath));
        // 프로필 이미지
        registry.addResourceHandler(toPattern(profileUrlPrefix))
                .addResourceLocations(resolveLocation(profileImagePath));
    }

    /**
     * URL 패턴을 '**' 와 함께 반환
     */
    private String toPattern(String prefix) {
        return (prefix.endsWith("/") ? prefix : prefix + "/") + "**";
    }

    /**
     * raw 경로(file: 생략 가능)를 프로젝트 루트 기준의 절대 파일 시스템 경로로 변환
     */
    private String resolveLocation(String raw) {
        if (raw.startsWith("classpath:") || raw.startsWith("file:/")) {
            return raw.endsWith("/") ? raw : raw + "/";
        }

        String sub = raw.startsWith("file:") ? raw.substring(5) : raw;
        Path root = Paths.get(System.getProperty("user.dir"));
        Path abs = root.resolve(sub).normalize();
        createDir(abs);
        return "file:" + abs.toString() + "/";
    }

    /**
     * 디렉터리가 없으면 생성
     */
    private void createDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
    }
}
