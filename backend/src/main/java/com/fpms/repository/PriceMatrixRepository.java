package com.fpms.repository;

import com.fpms.entity.PriceMatrix;
import com.fpms.entity.enums.DayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceMatrixRepository extends JpaRepository<PriceMatrix, Long> {
    List<PriceMatrix> findByDayType(DayType dayType);
    Optional<PriceMatrix> findByPitchTypeIdAndIsPeakHourAndDayType(
            Long pitchTypeId,
            Boolean isPeakHour,
            DayType dayType
    );

    List<PriceMatrix> findAllByDayType(DayType dayType);
}
