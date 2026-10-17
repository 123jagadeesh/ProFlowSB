package com.proflow.repository;

import com.proflow.entity.ProjectAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAttachmentRepository extends JpaRepository<ProjectAttachment, Long> {

    List<ProjectAttachment> findByProjectIdOrderByUploadedAtDesc(Long projectId);

    Optional<ProjectAttachment> findByProjectIdAndStoredFilename(Long projectId, String storedFilename);
}
