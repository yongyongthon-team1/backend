package com.uniro.backend.domain.store.service;

import com.uniro.backend.common.ApiException;
import com.uniro.backend.domain.store.dto.StoreSearchItemResponse;
import com.uniro.backend.domain.store.entity.Store;
import com.uniro.backend.domain.store.repository.StoreRepository;
import com.uniro.backend.domain.store.support.StoreErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final StoreRepository storeRepository;

    // 상호로 상점 검색 (사장님이 내 가게를 연결할 때 사용)
    public Map<String, Object> search(String keyword, Integer page, Integer size) {
        String trimmed = keyword == null ? "" : keyword.trim();

        if (trimmed.length() < 2) {
            throw StoreErrors.invalid("keyword", "검색어는 2자 이상 입력해 주세요");
        }

        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            throw StoreErrors.invalid(
                    "keyword",
                    "검색어는 " + MAX_KEYWORD_LENGTH + "자 이하로 입력해 주세요"
            );
        }

        int pageNo = page == null ? 1 : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;

        if (pageNo < 1) {
            throw StoreErrors.invalid("page", "page는 1 이상이어야 합니다");
        }
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw StoreErrors.invalid("size", "size는 1~" + MAX_SIZE + " 사이여야 합니다");
        }

        Page<Store> result = storeRepository.findByNameContaining(
                trimmed,
                PageRequest.of(pageNo - 1, pageSize, Sort.by("name", "id"))
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", result.getContent().stream()
                .map(s -> new StoreSearchItemResponse(
                        s.getId(),
                        s.getName(),
                        s.getAddress(),
                        s.getCategory(),
                        s.getOwnerId() != null
                ))
                .toList());
        data.put("page", pageNo);
        data.put("size", pageSize);
        data.put("total", result.getTotalElements());
        return data;
    }
}
