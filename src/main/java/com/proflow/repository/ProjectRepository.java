package com.proflow.repository;

import com.proflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    Optional<Project> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByNameIgnoreCaseAndCompanyId(String name, Long companyId);

    // For employee view: projects where employee has assigned tasks
    @Query("SELECT DISTINCT p FROM Project p JOIN Task t ON t.project.id = p.id JOIN t.assignee a WHERE a.id = :userId AND p.company.id = :companyId ORDER BY p.createdAt DESC")
    List<Project> findProjectsForEmployee(@Param("userId") Long userId, @Param("companyId") Long companyId);

    long countByCompanyId(Long companyId);

    // Global count for super admin
    long count();
}
