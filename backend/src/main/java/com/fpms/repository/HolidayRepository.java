package com.fpms.repository;

import com.fpms.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    boolean existsByHolidayDate(LocalDate holidayDate);
}
