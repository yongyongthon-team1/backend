package com.uniro.backend.domain.review.service;

import com.uniro.backend.domain.review.event.ReviewCreatedEvent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// 리뷰가 커밋된 뒤, 리뷰 작성 응답과 별개로 요약을 갱신한다.
// 전용 스레드풀을 이 클래스 안에서만 쓴다. 전역 @Async 설정이나 공용 Executor 빈을 만들지 않아,
// 다른 기능의 비동기 처리에 영향을 주지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSummaryListener {

    private static final int THREADS = 2;
    private static final int QUEUE_SIZE = 50;

    private final ReviewSummaryService summaryService;

    // 대기열이 차면 새 요약 요청은 거절하고 기록한다(요약은 다음 리뷰 때 다시 시도된다)
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            THREADS, THREADS, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(QUEUE_SIZE),
            threadFactory());

    // 서버가 꺼질 때 요약 작업 때문에 멈추지 않도록 데몬 스레드로 둔다
    private static CustomizableThreadFactory threadFactory() {
        CustomizableThreadFactory factory = new CustomizableThreadFactory("review-summary-");
        factory.setDaemon(true);
        return factory;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCreated(ReviewCreatedEvent event) {
        try {
            executor.execute(() -> refresh(event.storeId()));
        } catch (RejectedExecutionException e) {
            log.warn("리뷰 요약 대기열이 가득 차서 건너뜁니다(store {})", event.storeId());
        }
    }

    private void refresh(Long storeId) {
        try {
            summaryService.refreshIfNeeded(storeId);
        } catch (RuntimeException e) {
            // 요약이 실패해도 리뷰 저장에는 영향이 없다. 다음 리뷰 때 다시 시도된다.
            log.warn("리뷰 요약 갱신 실패(store {}): {}", storeId, e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
