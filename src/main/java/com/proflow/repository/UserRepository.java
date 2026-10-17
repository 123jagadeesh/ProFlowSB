package com.proflow.repository;

import com.proflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByCompanyIdAndRoleIgnoreCase(Long companyId, String role);

    List<User> findByCompanyId(Long companyId);

    // Zoom invitees: all users in company except the current user with roles employee/admin
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.id <> :currentUserId AND LOWER(u.role) IN :roles")
    List<User> findZoomInvitees(
            @Param("companyId") Long companyId,
            @Param("currentUserId") Long currentUserId,
            @Param("roles") Collection<String> roles
    );

    // For Super Admin / Root User:
    List<User> findByRoleIgnoreCase(String role);

    long countByRoleIgnoreCase(String role);

    long countByCompanyId(Long companyId);
}
