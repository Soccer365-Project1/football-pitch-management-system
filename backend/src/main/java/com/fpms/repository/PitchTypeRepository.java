package com.fpms.repository;

import com.fpms.entity.PitchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PitchTypeRepository extends JpaRepository<PitchType, Long> {

    Optional<PitchType> findByIdAndIsDeletedFalse(Long id);

    Optional<PitchType> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    List<PitchType> findAllByIsDeletedFalse();
}
