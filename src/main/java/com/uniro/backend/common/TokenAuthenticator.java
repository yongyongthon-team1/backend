package com.uniro.backend.common;

import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// Authorization 헤더에서 로그인한 사용자를 꺼내는 공통 헬퍼
@Component
@RequiredArgsConstructor
public class TokenAuthenticator {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    // 토큰이 유효하고 해당 사용자가 아직 존재할 때만 사용자 ID를 돌려준다
    public Long requireUserId(String authorization) {
        return requireUser(authorization).getId();
    }

    // 역할(role) 확인이 필요한 API에서 사용자 전체를 돌려준다
    public User requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw unauthorized();
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw unauthorized();
        }

        Long userId;

        try {
            userId = jwtProvider.getUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }

        return userRepository.findById(userId)
                .orElseThrow(this::unauthorized);
    }

    private ApiException unauthorized() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "E401",
                "토큰이 없거나 만료되었습니다"
        );
    }
}
