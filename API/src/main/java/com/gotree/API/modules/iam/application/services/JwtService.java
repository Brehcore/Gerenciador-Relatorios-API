package com.gotree.API.modules.iam.application.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;


/**
 * Serviço responsável por gerenciar operações relacionadas a JSON Web Tokens (JWT).
 * Fornece funcionalidades para geração, validação e extração de informações de tokens.
 * Classe dedicada apenas ao VPS
 */
@Service
public class JwtService {


    @Value("${jwt.secret_path}")
    private String secretKeyPath;

    private static final long EXPIRATION_TIME = 86_400_000L; // 1 dia

    /**
     * Gera um token JWT para um usuário específico.
     *
     * @param userDetails detalhes do usuário para quem o token será gerado
     * @return String contendo o token JWT gerado
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        extraClaims.put("roles", roles);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey()) // O algoritmo (HS256) agora é deduzido automaticamente da chave
                .compact();
    }

    /**
     * Verifica se um token JWT é válido para um determinado usuário.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExperired(token);
    }

    /**
     * Extrai o nome do usuário do token JWT.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Verifica se um token JWT está expirado.
     */
    private boolean isTokenExperired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrai a data de expiração do token JWT.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrai uma informação específica do token JWT.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrai todas as claims (informações) do token JWT.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gera uma chave de assinatura a partir da chave secreta.
     *
     * @return SecretKey objeto contendo a chave de assinatura simétrica
     */
    private SecretKey getSignInKey() {
        try {
            String content;

            // Se for caminho de arquivo (Docker Secrets)
            if (secretKeyPath != null && secretKeyPath.startsWith("/")) {
                content = Files.readString(Paths.get(secretKeyPath)).trim();
                System.out.println("DEBUG JWT: Chave lida do arquivo. Tamanho: " + content.length());
            } else {
                content = secretKeyPath;
            }

            if (content == null || content.isBlank()) {
                throw new RuntimeException("A chave JWT lida está vazia! Verifique o caminho: " + secretKeyPath);
            }

            byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(content);

            if (keyBytes.length < 32) {
                throw new RuntimeException("A chave JWT precisa ter pelo menos 256 bits (32 caracteres).");
            }

            return Keys.hmacShaKeyFor(keyBytes);

        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler o arquivo do segredo em: " + secretKeyPath, e);
        }
    }
}