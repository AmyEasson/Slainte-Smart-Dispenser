package com.vitamindispenser.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Handles generating and validating tokens using the jjwt library.
 * These tokens will be used for user authentication.
 */

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String SECRET;

    public String generateToken(String username){
        //24 hours
        long EXPIRY = 1000L * 60 * 60 * 24;
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            extractUsername(token);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return false; // token expired
        } catch (io.jsonwebtoken.JwtException e) {
            return false; // token invalid/malformed
        }
    }

}
