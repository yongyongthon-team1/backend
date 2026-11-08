package com.uniro.backend.common;

import com.uniro.backend.config.JwtProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// Authorization 헤더에서 사용자 ID를 꺼내는 공통 헬퍼
@Component
@RequiredArgsConstructor
public class TokenAuthenticator {

    private final JwtProvider jwtProvider;

    public Long requireUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw unauthorized();
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw unauthorized();
        }

        try {
            return jwtProvider.getUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }
    }

    private ApiException unauthorized() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "E401",
                "토큰이 없거나 만료되었습니다"
        );
    }
}
