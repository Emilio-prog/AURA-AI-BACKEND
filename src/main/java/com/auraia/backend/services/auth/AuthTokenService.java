package com.auraia.backend.services.auth;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.mappers.UserMapper;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.entities.RefreshToken;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.RefreshTokenRepository;
import com.auraia.backend.security.jwt.JwtTokenProvider;
import com.auraia.backend.utils.TokenHashing;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final AppProperties properties;

    public AuthResponses.AuthResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = TokenHashing.newOpaqueToken();
        refreshTokenRepository.save(RefreshToken.builder()
            .user(user)
            .tokenHash(TokenHashing.sha256(refreshToken))
            .expiresAt(Instant.now().plusMillis(properties.getJwt().getRefreshTokenExpirationMs()))
            .build());
        return new AuthResponses.AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            jwtTokenProvider.accessTokenExpirationMs(),
            userMapper.toResponse(user)
        );
    }
}
