package com.uniro.backend.domain.store.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.store.dto.MyStoreResponse;
import com.uniro.backend.domain.store.dto.PartnershipRequest;
import com.uniro.backend.domain.store.dto.StoreRegisterRequest;
import com.uniro.backend.domain.store.dto.StoreResponse;
import com.uniro.backend.domain.store.dto.StoreUpdateRequest;
import com.uniro.backend.domain.store.entity.Store;
import com.uniro.backend.domain.store.entity.StorePartnership;
import com.uniro.backend.domain.store.repository.StorePartnershipRepository;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import com.uniro.backend.domain.user.entity.User;
import com.uniro.backend.domain.verification.entity.BusinessVerification;
import com.uniro.backend.domain.verification.repository.BusinessVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;

// 사장님의 내 가게 등록·조회·수정
@Service
@RequiredArgsConstructor
public class MyStoreService {

    private final StoreRepository storeRepository;
    private final StorePartnershipRepository partnershipRepository;
    private final BusinessVerificationRepository businessVerificationRepository;

    // 가게 등록 (사장님 계정 연결)
    @Transactional
    public StoreResponse register(User user, StoreRegisterRequest request) {
        boolean verified = businessVerificationRepository.findById(user.getId())
                .filter(BusinessVerification::isVerified)
                .isPresent();

        if (!verified) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "E403_VERIFICATION_REQUIRED",
                    "사업자 인증 후 이용할 수 있어요"
            );
        }

        // 사장님 한 명당 가게 하나
        if (storeRepository.existsByOwnerId(user.getId())) {
            throw unavailable();
        }

        Store store = request.storeId() != null
                ? connectExisting(user, request.storeId())
                : createDirectly(user, request);

        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getCategory()
        );
    }

    // 내 가게 조회
    @Transactional(readOnly = true)
    public MyStoreResponse getMine(User user) {
        return toResponse(findMine(user));
    }

    // 가게 수정 (보낸 필드만)
    @Transactional
    public MyStoreResponse updateMine(User user, StoreUpdateRequest request) {
        if (request.name() == null
                && request.address() == null
                && request.category() == null) {
            throw StoreErrors.invalid("body", "수정할 항목을 하나 이상 보내 주세요");
        }

        Store store = findMine(user);

        String name = optionalText("name", request.name(), Store.NAME_MAX);
        String address = optionalText("address", request.address(), Store.ADDRESS_MAX);
        String category = optionalText("category", request.category(), Store.CATEGORY_MAX);

        String newName = Objects.requireNonNullElse(name, store.getName());
        String newAddress = Objects.requireNonNullElse(address, store.getAddress());

        // 상호나 주소가 바뀔 때만 다른 상점과 겹치는지 확인
        boolean keyChanged = !newName.equals(store.getName())
                || !newAddress.equals(store.getAddress());

        if (keyChanged && storeRepository.existsByNameAndAddressAndIdNot(
                newName, newAddress, store.getId())) {
            throw duplicated();
        }

        store.update(name, address, category);

        return toResponse(flush(store, this::duplicated));
    }

    // 제휴 할인 설정 (없으면 만들고, 있으면 덮어쓴다)
    @Transactional
    public MyStoreResponse.Partnership savePartnership(
            User user, PartnershipRequest request
    ) {
        String discount = requiredText(
                "discount_text", request.discountText(), StorePartnership.DISCOUNT_MAX);

        if (request.startDate() == null) {
            throw StoreErrors.invalid("start_date", "필수 값입니다");
        }
        if (request.endDate() == null) {
            throw StoreErrors.invalid("end_date", "필수 값입니다");
        }
        StoreErrors.requireReasonableDate("start_date", request.startDate());
        StoreErrors.requireReasonableDate("end_date", request.endDate());

        if (request.endDate().isBefore(request.startDate())) {
            throw StoreErrors.invalid("end_date", "시작일과 같거나 이후여야 합니다");
        }

        Store store = findMine(user);

        StorePartnership partnership = partnershipRepository
                .findByStoreId(store.getId())
                .map(existing -> {
                    existing.update(discount, request.startDate(), request.endDate());
                    return existing;
                })
                .orElseGet(() -> new StorePartnership(
                        store.getId(), discount, request.startDate(), request.endDate()));

        try {
            partnershipRepository.saveAndFlush(partnership);
        } catch (DataIntegrityViolationException e) {
            // 입력 값은 위에서 모두 검증했으므로, 남은 원인은
            // 같은 가게의 제휴를 동시에 처음 저장한 경우(store_id 유니크)뿐이다
            throw new ApiException(
                    HttpStatus.CONFLICT, "E409", "잠시 후 다시 시도해 주세요");
        }

        return MyStoreResponse.Partnership.from(partnership);
    }

    // 상점 검색에서 고른 상점을 연결
    private Store connectExisting(User user, Long storeId) {
        Store store = storeRepository.findByIdForUpdate(storeId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "존재하지 않는 상점입니다"));

        if (store.getOwnerId() != null) {
            throw unavailable();
        }

        store.assignOwner(user.getId());

        return flush(store, this::unavailable);
    }

    // 목록에 없는 가게를 직접 입력해 등록
    private Store createDirectly(User user, StoreRegisterRequest request) {
        String name = requiredText("name", request.name(), Store.NAME_MAX);
        String address = requiredText("address", request.address(), Store.ADDRESS_MAX);
        String category = requiredText("category", request.category(), Store.CATEGORY_MAX);
        BigDecimal latitude = range("latitude", request.latitude(), 90);
        BigDecimal longitude = range("longitude", request.longitude(), 180);

        if (storeRepository.existsByNameAndAddress(name, address)) {
            throw unavailable();
        }

        return flush(
                new Store(user.getId(), name, address, category, latitude, longitude),
                this::unavailable
        );
    }

    // 동시 요청으로 유니크 제약에 걸리면 호출한 쪽이 정한 409 오류로 바꾼다
    private Store flush(Store store, Supplier<ApiException> onConflict) {
        try {
            return storeRepository.saveAndFlush(store);
        } catch (DataIntegrityViolationException e) {
            throw onConflict.get();
        }
    }

    private Store findMine(User user) {
        return storeRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "E404", "연결한 가게가 없습니다"));
    }

    private MyStoreResponse toResponse(Store store) {
        return new MyStoreResponse(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getCategory(),
                partnershipRepository.findByStoreId(store.getId())
                        .map(MyStoreResponse.Partnership::from)
                        .orElse(null)
        );
    }

    // 보내지 않았으면 null, 보냈으면 공백을 없애고 길이를 확인한다
    private String optionalText(String field, String value, int max) {
        return value == null ? null : requiredText(field, value, max);
    }

    private String requiredText(String field, String value, int max) {
        String trimmed = value == null ? "" : value.trim();

        if (trimmed.isEmpty()) {
            throw StoreErrors.invalid(field, "필수 값입니다");
        }
        if (trimmed.length() > max) {
            throw StoreErrors.invalid(field, max + "자 이하로 입력해 주세요");
        }

        return trimmed;
    }

    private BigDecimal range(String field, BigDecimal value, int limit) {
        if (value == null) {
            throw StoreErrors.invalid(field, "필수 값입니다");
        }
        if (value.abs().compareTo(BigDecimal.valueOf(limit)) > 0) {
            throw StoreErrors.invalid(field, "-" + limit + " ~ " + limit + " 사이여야 합니다");
        }

        return value;
    }

    private ApiException unavailable() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "E409_STORE_UNAVAILABLE",
                "연결할 수 없는 상점입니다"
        );
    }

    private ApiException duplicated() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "E409",
                "같은 상호와 주소의 상점이 이미 있습니다"
        );
    }
}
