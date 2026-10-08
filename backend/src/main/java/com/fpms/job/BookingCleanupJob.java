package com.fpms.job;

import com.fpms.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import com.fpms.dto.response.BookingEventDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupJob {

    private final BookingRepository bookingRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void cleanupExpiredBookings() {
        int cancelledCount = bookingRepository.cancelExpiredHolds(java.time.LocalDateTime.now());
        if (cancelledCount > 0) {
            log.info("[BookingCleanupJob] Successfully auto-cancelled {} expired PENDING_HOLD bookings.", cancelledCount);
            
            // Notify clients to refresh the schedule grid ONLY AFTER transaction commits
            BookingEventDto event = BookingEventDto.builder()
                    .action("BOOKINGS_CANCELLED")
                    .status("CANCELLED")
                    .build();
                    
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend("/topic/schedule", event);
                }
            });
        }
    }
}
