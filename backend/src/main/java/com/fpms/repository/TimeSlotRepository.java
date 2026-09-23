package com.fpms.repository;

import com.fpms.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findAllByOrderByStartTimeAsc();

    List<TimeSlot> findAllByIsActiveTrueOrderByStartTimeAsc();

    Optional<TimeSlot> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TimeSlot t " +
           "WHERE t.isActive = true " +
           "AND (:id IS NULL OR t.id != :id) " +
           "AND (:startTime < t.endTime AND t.startTime < :endTime)")
    boolean existsOverlappingSlot(@Param("id") Long id,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime);
}
