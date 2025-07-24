package com.cpt202.dailyreadingtracker.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

// Utility class for JSON Web Token generation, validation, and processing operations

@Component
public class JWTTokenUtil {
    
    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expirationMs}")
    private long expirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init(){
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject){
        return Jwts.builder()
                   .subject(subject)
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + expirationMs))
                   .signWith(secretKey, Jwts.SIG.HS256)
                   .compact();
    }

    public boolean validateToken(String token){
        try{
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token){
        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload()
                   .getSubject();
    }

}
