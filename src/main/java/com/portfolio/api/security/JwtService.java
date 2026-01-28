package com.portfolio.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private static final String ISSUER = "portfolio-api";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generateToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(ISSUER)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(Claims claims, UserDetails userDetails) {
        String username = claims.getSubject();
        Date expiration = claims.getExpiration();
        String issuer = claims.getIssuer();

        return username != null
                && username.equals(userDetails.getUsername())
                && expiration != null
                && expiration.after(new Date())
                && ISSUER.equals(issuer);
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256-bit (32 bytes). Generate with: openssl rand -base64 32");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
