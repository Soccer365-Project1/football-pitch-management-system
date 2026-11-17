package com.fpms.repository;

import com.fpms.entity.BookingCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingCancellationRepository extends JpaRepository<BookingCancellation, Long> {
    Optional<BookingCancellation> findByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
    List<BookingCancellation> findByBookingIdIn(Collection<Long> bookingIds);
}
