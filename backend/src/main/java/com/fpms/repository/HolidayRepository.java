package com.fpms.repository;

import com.fpms.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByHolidayDate(LocalDate holidayDate);

    boolean existsByHolidayDateAndIdNot(LocalDate holidayDate, Long id);

    List<Holiday> findAllByOrderByHolidayDateAsc();

    @Query("SELECT h FROM Holiday h WHERE EXTRACT(YEAR FROM h.holidayDate) = :year ORDER BY h.holidayDate ASC")
    List<Holiday> findAllByYearOrderByHolidayDateAsc(@Param("year") int year);

    Optional<Holiday> findByHolidayDate(LocalDate holidayDate);
}
