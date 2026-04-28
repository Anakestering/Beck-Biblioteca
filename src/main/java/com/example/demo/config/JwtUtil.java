package com.example.demo.config;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
 
import javax.crypto.SecretKey;
import java.util.Date;
 
/**
 * Classe utilitária para operações com JWT (JSON Web Token).
 * Responsável por gerar, validar e extrair informações dos tokens.
 */
@Component
public class JwtUtil {
 
    @Value("${jwt.secret}")
    private String secret;
 
    private SecretKey key;
 
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Gera um token JWT para o usuário informado.
     * @param username Nome do usuário para o qual o token será gerado
     * @return Token JWT como String
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 4))
                .signWith(key)
                .compact();
    }

    public String extractRole(String token) {
        return (String) getClaims(token).getPayload().get("role");
    }

    /**
     * Extrai o nome de usuário (subject) de um token JWT.
     * @param token Token JWT
     * @return Nome do usuário contido no token
     */
    public String extractUsername(String token) {
        return getClaims(token).getPayload().getSubject();
    }

    /**
     * Valida se o token JWT é válido (assinatura e expiração).
     * @param token Token JWT
     * @return true se válido, false caso contrário
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            System.out.println("JWT inválido: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtém as claims (informações) do token JWT, validando a assinatura.
     * @param token Token JWT
     * @return Claims do token
     */
    private Jws<Claims> getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key) // Define a chave para validação
                .build()
                .parseSignedClaims(token); // Faz o parsing e validação do token
    }

}