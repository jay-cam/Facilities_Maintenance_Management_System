package com.facilitymaintenance.backend.repository;

import com.facilitymaintenance.backend.model.WorkOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderHistoryRepository extends JpaRepository<WorkOrderHistory, Long>{
    List<WorkOrderHistory> findByMaintenanceRequestIdOrderByCreatedAtAsc(Long requestId);
}
