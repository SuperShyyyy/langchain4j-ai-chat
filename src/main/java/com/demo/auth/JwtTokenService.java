package com.demo.auth;

import com.demo.config.AppJwtProperties;
import com.demo.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final AppJwtProperties appJwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(AppJwtProperties appJwtProperties) {
        this.appJwtProperties = appJwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(appJwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(appJwtProperties.getExpirationSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(appJwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("username", username)
                .signWith(secretKey)
                .compact();
    }

    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        }
    }

    public long getExpirationSeconds() {
        return appJwtProperties.getExpirationSeconds();
    }
}
