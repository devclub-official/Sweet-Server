package com.example.ptpt.util;

import com.example.ptpt.enums.ApiResponseCode;
import com.example.ptpt.exception.AuthServiceException;
import com.example.ptpt.exception.token.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_KEY = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";

    private final SecretKey secretKey;
    private final JwtParser jwtParser;


    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .setSigningKey(secretKey)
                .build();
    }

    // ===== Token Validation Methods =====

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰");
            throw new ExpiredTokenException();
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰");
            throw new UnsupportedTokenException();
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰");
            throw new MalformedTokenException();
        } catch (SignatureException e) {
            log.warn("잘못된 서명의 JWT 토큰");
            throw new InvalidSignatureTokenException();
        } catch (JwtException e) {
            log.warn("잘못된 JWT 토큰");
            throw new InvalidTokenException();
        }
    }

    /**
     * Access Token인지 확인
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return TOKEN_TYPE_ACCESS.equals(claims.get(TOKEN_TYPE_KEY));
        } catch (Exception e) {
            return false;
        }
    }

    // ===== Token Information Extraction Methods =====

    /**
     * 토큰에서 이메일(subject) 추출
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 토큰에서 만료 시간 추출
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 토큰에서 특정 claim 추출
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Authorization 헤더에서 JWT 토큰을 추출하고 사용자 ID를 반환
     */
    public Long extractUserIdFromToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new AuthServiceException(ApiResponseCode.AUTH_TOKEN_INVALID, "유효하지 않은 토큰입니다.");
        }

        String token = authorization.substring(7);

        Long userId = null;
        try {
            // JWT에서 userId claim 추출 (String으로 저장된 경우)
            userId = extractClaim(token, claims -> claims.get("userId", Long.class));

            if (userId == null) {
                throw new AuthServiceException(ApiResponseCode.AUTH_TOKEN_INVALID, "토큰에 사용자 정보가 없습니다.");
            }

            return userId;

        } catch (NumberFormatException e) {
            log.error("사용자 ID 파싱 실패: {}", userId, e);
            throw new AuthServiceException(ApiResponseCode.AUTH_TOKEN_INVALID, "토큰의 사용자 정보가 올바르지 않습니다.");
        } catch (Exception e) {
            log.error("토큰 파싱 실패", e);
            throw new AuthServiceException(ApiResponseCode.AUTH_TOKEN_INVALID, "토큰 파싱에 실패했습니다.");
        }
    }


    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 토큰에서 모든 Claims 추출
     */
    private Claims extractAllClaims(String token) {
        try {
            return jwtParser
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (UnsupportedJwtException e) {
            throw new UnsupportedTokenException();
        } catch (MalformedJwtException e) {
            throw new MalformedTokenException();
        } catch (SignatureException e) {
            throw new InvalidSignatureTokenException();
        } catch (JwtException e) {
            throw new InvalidTokenException();
        }
    }
}