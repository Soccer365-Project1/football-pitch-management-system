package com.fpms.security;

import com.fpms.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return generateAccessToken(user.getId(), user.getEmail(), user.getRole().getRoleName().name());
    }

    public String generateAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        return generateRefreshToken(user.getId());
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getClaimsFromJWT(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = getClaimsFromJWT(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj != null) {
            return Long.valueOf(userIdObj.toString());
        }
        return Long.valueOf(claims.getSubject());
    }

    public String getEmailFromJWT(String token) {
        Claims claims = getClaimsFromJWT(token);
        return claims.get("email", String.class);
    }

    public String getEmailFromToken(String token) {
        return getEmailFromJWT(token);
    }

    public String getRoleFromJWT(String token) {
        Claims claims = getClaimsFromJWT(token);
        return claims.get("role", String.class);
    }

    public Date getExpirationFromJWT(String token) {
        return getClaimsFromJWT(token).getExpiration();
    }

    public long getRemainingExpirationMs(String token) {
        Date expiration = getExpirationFromJWT(token);
        long diff = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, diff);
    }

    public void blacklistToken(String token) {
        try {
            long remainingMs = getRemainingExpirationMs(token);
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + token,
                        "blacklisted",
                        remainingMs,
                        TimeUnit.MILLISECONDS
                );
                log.info("Đã đưa token vào Redis blacklist với TTL = {} ms", remainingMs);
            }
        } catch (Exception ex) {
            log.error("Lỗi khi lưu token vào Redis blacklist: {}", ex.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception ex) {
            log.error("Lỗi khi kiểm tra Redis blacklist: {}", ex.getMessage());
            return false;
        }
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);

            if (isBlacklisted(authToken)) {
                log.warn("Mã xác thực JWT đã bị thu hồi (người dùng đã đăng xuất)");
                return false;
            }

            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            log.error("Chữ ký JWT không hợp lệ: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Mã xác thực JWT đã hết hạn: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Mã xác thực JWT không được hỗ trợ: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("Chuỗi JWT claims rỗng hoặc không hợp lệ: {}", ex.getMessage());
        }
        return false;
    }
}
