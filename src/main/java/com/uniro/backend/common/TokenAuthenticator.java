package com.uniro.backend.common;

import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
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
    // (역할이 필요 없는 API용: 사용자 행 전체를 읽지 않고 존재만 확인)
    public Long requireUserId(String authorization) {
        Long userId = parseUserId(authorization);

        if (!userRepository.existsById(userId)) {
            throw unauthorized();
        }

        return userId;
    }

    // 역할(role) 확인이 필요한 API에서 사용자 전체를 돌려준다
    public User requireUser(String authorization) {
        return userRepository.findById(parseUserId(authorization))
                .orElseThrow(this::unauthorized);
    }

    // 사장님만 쓸 수 있는 API용: 로그인한 사용자가 사장님이 아니면 403
    public User requireOwner(String authorization) {
        return requireRole(authorization, UserRole.OWNER, "사장님만 이용할 수 있습니다");
    }

    // 고교생만 쓸 수 있는 API용: 로그인한 사용자가 고교생이 아니면 403
    public User requireHighSchool(String authorization) {
        return requireRole(authorization, UserRole.HIGH_SCHOOL, "고교생만 이용할 수 있습니다");
    }

    private User requireRole(String authorization, UserRole role, String message) {
        User user = requireUser(authorization);

        if (user.getRole() != role) {
            throw new ApiException(HttpStatus.FORBIDDEN, "E403", message);
        }

        return user;
    }

    private Long parseUserId(String authorization) {
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
