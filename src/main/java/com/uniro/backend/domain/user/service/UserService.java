package com.uniro.backend.domain.user.service;

import com.uniro.backend.config.JwtProvider;
import com.uniro.backend.domain.user.dto.*;
import com.uniro.backend.domain.user.entity.*;
import com.uniro.backend.domain.user.repository.*;
import com.uniro.backend.domain.guardianconsent.repository.GuardianConsentRepository;
import com.uniro.backend.domain.university.repository.UniversityRepository;
import com.uniro.backend.domain.verification.entity.VerificationStatus;
import com.uniro.backend.domain.verification.repository.StudentVerificationRepository;
import com.uniro.backend.domain.verification.repository.BusinessVerificationRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final GuideProfileRepository guideProfileRepository;
    private final GuardianConsentRepository guardianConsentRepository;
    private final UniversityRepository universityRepository;

    private final StudentVerificationRepository studentVerificationRepository;
    private final BusinessVerificationRepository businessVerificationRepository;

    private final JwtProvider jwtProvider;

    @Transactional(readOnly = true)
    public UserMeResponse getMyInfo(String authorization) {

        // 1. Authorization 헤더 확인
        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            throw unauthorized();
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw unauthorized();
        }

        // 2. JWT 검증 및 사용자 ID 추출
        Long userId;

        try {
            userId = jwtProvider.getUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(this::unauthorized);

        String verificationStatus = VerificationStatus.NONE.name();
        String rejectReason = null;
        String schoolName = null;
        Integer grade = null;
        String interestMajor = null;
        String guardianConsentStatus = null;
        GuideProfileResponse guideProfile = null;

        // 4. 고교생 또는 대학생 인증 상태
        if (user.getRole() == UserRole.HIGH_SCHOOL ||
                user.getRole() == UserRole.UNIVERSITY) {

            var verification =
                    studentVerificationRepository.findById(userId);

            if (verification.isPresent()) {
                var v = verification.get();

                verificationStatus = v.getStatus().name();

                if (v.getStatus() == VerificationStatus.REJECTED) {
                    rejectReason = v.getRejectionReason();
                }
            }
        }

        // 5. 고교생 정보
        if (user.getRole() == UserRole.HIGH_SCHOOL) {

            var profile = studentProfileRepository.findById(userId);

            if (profile.isPresent()) {
                StudentProfile p = profile.get();

                schoolName = p.getHighSchoolName();
                grade = p.getGrade();
                interestMajor = p.getInterestedMajor();
            }

            guardianConsentStatus =
                    guardianConsentRepository.findById(userId)
                            .map(consent -> consent.getStatus())
                            .orElse(null);
        }

        // 6. 대학생 정보
        if (user.getRole() == UserRole.UNIVERSITY) {

            var profile = guideProfileRepository.findById(userId);

            if (profile.isPresent()) {
                GuideProfile p = profile.get();

                guideProfile = GuideProfileResponse.from(p);

                if (p.getUniversityId() != null) {
                    schoolName = universityRepository
                            .findById(p.getUniversityId())
                            .map(university ->
                                    university.getName() + " " +
                                            university.getCampusName()
                            )
                            .orElse(null);
                }
            }
        }

        // 7. 사장님 인증 상태
        if (user.getRole() == UserRole.OWNER) {

            verificationStatus =
                    businessVerificationRepository.findById(userId)
                            .map(verification ->
                                    verification.isVerified()
                                            ? VerificationStatus.APPROVED.name()
                                            : VerificationStatus.NONE.name()
                            )
                            .orElse(VerificationStatus.NONE.name());
        }

        // 8. 응답 생성
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                verificationStatus,
                rejectReason,
                schoolName,
                grade,
                interestMajor,
                guardianConsentStatus,
                guideProfile
        );
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "유효하지 않거나 만료된 토큰입니다."
        );
    }

    @Transactional
    public UserMeResponse updateMyInfo(
            String authorization,
            Map<String, Object> request
    ) {
        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            throw unauthorized();
        }

        String token = authorization.substring(7).trim();

        Long userId;

        try {
            userId = jwtProvider.getUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(this::unauthorized);

        if (request == null) {
            throw badRequest("요청 본문이 필요합니다.");
        }

        for (String field : request.keySet()) {
            if (!field.equals("name") &&
                    !field.equals("school_name") &&
                    !field.equals("grade") &&
                    !field.equals("interest_major")) {
                throw badRequest("수정할 수 없는 필드입니다: " + field);
            }
        }

        String newName = null;
        String newSchoolName = null;
        Integer newGrade = null;
        String newInterestMajor = null;

        // 이름 검증
        if (request.containsKey("name")) {
            Object value = request.get("name");

            if (!(value instanceof String name) ||
                    name.isBlank() ||
                    name.length() > 20) {
                throw badRequest("이름은 1~20자로 입력해야 합니다.");
            }

            newName = name;
        }

        boolean hasStudentFields =
                request.containsKey("school_name") ||
                        request.containsKey("grade") ||
                        request.containsKey("interest_major");

        if (hasStudentFields &&
                user.getRole() != UserRole.HIGH_SCHOOL) {
            throw badRequest("고교생만 수정할 수 있는 필드입니다.");
        }

        // 고교생 전용 필드 검증
        if (request.containsKey("school_name")) {
            Object value = request.get("school_name");

            if (!(value instanceof String schoolName) ||
                    schoolName.isBlank() ||
                    schoolName.length() > 50) {
                throw badRequest("학교명은 1~50자로 입력해야 합니다.");
            }

            newSchoolName = schoolName;
        }

        if (request.containsKey("grade")) {
            Object value = request.get("grade");

            if (!(value instanceof Integer grade) ||
                    grade < 1 || grade > 3) {
                throw badRequest("학년은 1~3 사이의 정수여야 합니다.");
            }

            newGrade = grade;
        }

        if (request.containsKey("interest_major")) {
            Object value = request.get("interest_major");

            if (!(value instanceof String interestMajor) ||
                    interestMajor.isBlank() ||
                    interestMajor.length() > 30) {
                throw badRequest("관심 학과는 1~30자로 입력해야 합니다.");
            }

            newInterestMajor = interestMajor;
        }

        // 검증 완료 후 수정
        if (newName != null) {
            user.updateName(newName);
        }

        if (hasStudentFields) {
            StudentProfile profile =
                    studentProfileRepository.findById(userId)
                            .orElseGet(() -> new StudentProfile(userId));

            if (newSchoolName != null) {
                profile.updateHighSchoolName(newSchoolName);
            }

            if (newGrade != null) {
                profile.updateGrade(newGrade);
            }

            if (newInterestMajor != null) {
                profile.updateInterestedMajor(newInterestMajor);
            }

            studentProfileRepository.save(profile);
        }

        userRepository.save(user);

        return getMyInfo(authorization);
    }

    @Transactional
    public GuideProfileResponse saveGuideProfile(
            String authorization,
            GuideProfileRequest request
    ) {
        // JWT 확인
        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            throw unauthorized();
        }

        String token = authorization.substring(7).trim();

        Long userId;

        try {
            userId = jwtProvider.getUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }

        // 로그인 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(this::unauthorized);

        // 대학생만 등록 가능
        if (user.getRole() != UserRole.UNIVERSITY) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "대학생만 가이드 프로필을 등록할 수 있습니다."
            );
        }

        // 기존 프로필 조회
        GuideProfile profile = guideProfileRepository.findById(userId)
                .orElse(null);

        if (profile == null) {
            // 없으면 생성
            profile = new GuideProfile(
                    userId,
                    request.major(),
                    request.intro()
            );
        } else {
            // 있으면 수정
            profile.updateProfile(
                    request.major(),
                    request.intro()
            );
        }

        GuideProfile savedProfile =
                guideProfileRepository.save(profile);

        return GuideProfileResponse.from(savedProfile);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}