package com.fpms.repository;

import com.fpms.entity.Pitch;
import com.fpms.entity.enums.PitchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PitchRepository extends JpaRepository<Pitch, Long>, JpaSpecificationExecutor<Pitch> {

    Optional<Pitch> findByIdAndIsDeletedFalse(Long id);

    Optional<Pitch> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse(String name, Long id);

    List<Pitch> findAllByStatusAndIsDeletedFalse(PitchStatus status);

    List<Pitch> findAllByIsDeletedFalse();
}
