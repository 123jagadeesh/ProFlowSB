package com.proflow.repository;

import com.proflow.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<Sprint> findByIdAndCompanyId(Long id, Long companyId);

    List<Sprint> findByCompanyId(Long companyId);
}
