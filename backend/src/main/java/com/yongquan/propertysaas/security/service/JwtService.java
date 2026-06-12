package com.yongquan.propertysaas.security.service;

import com.yongquan.propertysaas.security.domain.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${property-saas.jwt.secret}") String secret,
            @Value("${property-saas.jwt.ttl-seconds}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(CurrentUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("tenantId", user.tenantId() == null ? "" : user.tenantId())
                .claim("username", user.username())
                .claim("realName", user.realName())
                .claim("userType", user.userType())
                .claim("permissions", user.permissions())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public CurrentUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object tenantValue = claims.get("tenantId");
        Long tenantId = tenantValue == null || tenantValue.toString().isBlank()
                ? null
                : Long.valueOf(tenantValue.toString());
        Object permissionsValue = claims.get("permissions");
        List<String> permissions = permissionsValue instanceof List<?> values
                ? values.stream().map(Object::toString).toList()
                : List.of();
        return new CurrentUser(
                Long.valueOf(claims.getSubject()),
                tenantId,
                claims.get("username", String.class),
                claims.get("realName", String.class),
                claims.get("userType", String.class),
                "ACTIVE",
                permissions
        );
    }
}
