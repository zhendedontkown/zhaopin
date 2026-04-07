package com.bishe.recruitment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(AuthenticatedUser user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("displayName", user.getDisplayName())
                .claim("roles", user.getRoles())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getAllClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object userId = getAllClaims(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(userId));
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return (List<String>) getAllClaims(token).get("roles");
    }

    public boolean validateToken(String token) {
        getAllClaims(token);
        return true;
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) getKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    private Key getKey() {
        String normalizedSecret = secret.length() >= 32 ? secret : (secret + "RecruitmentJwtSecretPadding1234567890");
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(normalizedSecret.getBytes())));
    }
}
