package org.alloy.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            // Invalid JWT signature
            logger.warn("Invalid JWT signature: {}", ex.getMessage());
            return false;
        } catch (MalformedJwtException ex) {
            // Invalid JWT token
            logger.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        } catch (ExpiredJwtException ex) {
            // Expired JWT token - это нормальная ситуация, логируем для отладки
            logger.info("JWT token expired for user: {}", ex.getClaims().getSubject());
            return false;
        } catch (UnsupportedJwtException ex) {
            // Unsupported JWT token
            logger.warn("Unsupported JWT token: {}", ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            // JWT claims string is empty
            logger.warn("JWT claims string is empty: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Проверяет, истек ли токен, и возвращает исключение, если истек
     */
    public boolean isTokenExpired(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
} 