package com.facilitymaintenance.backend.repository;

import com.facilitymaintenance.backend.model.WorkOrderComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderCommentRepository extends JpaRepository<WorkOrderComment, Long> {

    List<WorkOrderComment> findByMaintenanceRequestIdOrderByCreatedAtAsc(Long requestId);
}
