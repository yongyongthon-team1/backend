package com.uniro.backend.domain.auth.service;

import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.auth.dto.*;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.user.entity.UserRole;
import com.uniro.backend.domain.user.repository.UserRepository;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.repository.StudentVerificationRepository;
import com.uniro.backend.domain.verification.repository.BusinessVerificationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentVerificationRepository studentVerificationRepository;
    private final BusinessVerificationRepository businessVerificationRepository;
    private final JwtProvider jwtProvider;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {

        if (request.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "운영자 계정은 회원가입할 수 없습니다."
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다"
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        User user = new User(
                request.email(),
                encodedPassword,
                request.name(),
                request.role()
        );

        return SignupResponse.from(userRepository.save(user));
    }

    // 로그인
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(
                request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        VerificationStatus status = getVerificationStatus(user);

        String token = jwtProvider.generateToken(user.getId());

        return LoginResponse.from(token, user, status);
    }

    // 로그인 실패
    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "이메일 또는 비밀번호가 올바르지 않습니다"
        );
    }

    // 인증 상태 조회
    private VerificationStatus getVerificationStatus(User user) {

        Long userId = user.getId();

        if (user.getRole() == UserRole.HIGH_SCHOOL ||
                user.getRole() == UserRole.UNIVERSITY) {

            return studentVerificationRepository.findById(userId)
                    .map(verification -> verification.getStatus())
                    .orElse(VerificationStatus.NONE);
        }

        if (user.getRole() == UserRole.OWNER) {

            return businessVerificationRepository.findById(userId)
                    .map(verification ->
                            verification.isVerified()
                                    ? VerificationStatus.APPROVED
                                    : VerificationStatus.NONE
                    )
                    .orElse(VerificationStatus.NONE);
        }

        return VerificationStatus.NONE;
    }
}