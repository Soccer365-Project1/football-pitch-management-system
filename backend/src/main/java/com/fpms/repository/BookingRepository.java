package com.fpms.repository;

import com.fpms.entity.Booking;
import com.fpms.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByTimeSlotIdAndBookingDateGreaterThanEqualAndStatusInAndIsDeletedFalse(
            Long timeSlotId,
            LocalDate bookingDate,
            Collection<BookingStatus> statuses
    );
}
