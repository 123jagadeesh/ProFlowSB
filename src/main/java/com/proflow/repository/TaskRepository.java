package com.proflow.repository;

import com.proflow.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    Optional<Task> findByIdAndCompanyId(Long id, Long companyId);

    // Custom filtered query matching Express getTasks
    @Query("SELECT DISTINCT t FROM Task t " +
            "LEFT JOIN t.assignee a " +
            "WHERE t.company.id = :companyId " +
            "AND (:projectId IS NULL OR t.project.id = :projectId) " +
            "AND (:assigneeId IS NULL OR a.id = :assigneeId) " +
            "ORDER BY t.createdAt DESC")
    List<Task> findTasksWithFilter(
            @Param("companyId") Long companyId,
            @Param("projectId") Long projectId,
            @Param("assigneeId") Long assigneeId
    );

    boolean existsByProjectIdAndAssigneeIdAndCompanyId(Long projectId, Long userId, Long companyId);

    @Modifying
    @Query("UPDATE Task t SET t.sprint = null WHERE t.sprint.id = :sprintId")
    void clearSprintReference(@Param("sprintId") Long sprintId);

    long countByCompanyId(Long companyId);
}
