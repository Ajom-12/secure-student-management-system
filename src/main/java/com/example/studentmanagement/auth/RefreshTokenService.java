package com.example.studentmanagement.auth;

import com.example.studentmanagement.exception.InvalidCredentialsException;
import com.example.studentmanagement.config.JwtProperties;
import com.example.studentmanagement.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String create(User user) {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshToken().getExpiration()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public RefreshToken consume(String rawToken) {
        RefreshToken token = findActive(rawToken);
        if (token.getExpiryDate().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new InvalidCredentialsException("Refresh token has expired");
        }
        return token;
    }

    @Transactional
    public void revokeIfActive(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(this::revoke);
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private RefreshToken findActive(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked())
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token is invalid or revoked"));
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}