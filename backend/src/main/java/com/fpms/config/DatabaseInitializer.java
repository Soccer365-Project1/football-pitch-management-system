package com.fpms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndices() {
        log.info("Checking and initializing database indices...");
        try {
            // Drop index cũ nếu có để cập nhật lại điều kiện WHERE mới (thay thế REJECTED bằng REFUNDED)
            jdbcTemplate.execute("DROP INDEX IF EXISTS unique_active_booking");

            // Partial index for PostgreSQL to prevent double bookings
            // Ngoại trừ các đơn CANCELLED và REFUNDED, mỗi ca đá của 1 sân trong 1 ngày chỉ có tối đa 1 đơn đặt
            String sql = "CREATE UNIQUE INDEX IF NOT EXISTS unique_active_booking " +
                         "ON bookings (pitch_id, time_slot_id, booking_date) " +
                         "WHERE status NOT IN ('CANCELLED', 'REFUNDED')";
            jdbcTemplate.execute(sql);
            log.info("Partial unique index 'unique_active_booking' initialized successfully.");
        } catch (Exception e) {
            log.warn("Could not create partial unique index (This is expected if not using PostgreSQL or if syntax differs): {}", e.getMessage());
        }
    }
}
