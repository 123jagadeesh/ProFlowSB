package com.proflow.repository;

import com.proflow.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByCompanyIdOrderByStartTimeDesc(Long companyId);
}
