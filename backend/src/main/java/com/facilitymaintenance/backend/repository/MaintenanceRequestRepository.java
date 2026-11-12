package com.facilitymaintenance.backend.repository;

import com.facilitymaintenance.backend.model.MaintenanceRequest;
import com.facilitymaintenance.backend.model.RequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository responsible for database operations
 * involving maintenance requests.
 *
 * Spring Data JPA automatically generates the SQL
 * queries for methods following its naming conventions.
 */
public interface MaintenanceRequestRepository
        extends JpaRepository<MaintenanceRequest, Long> {

    /**
     * Retrieves all maintenance requests belonging
     * to a specific department.
     *
     * Used to restrict requesters and supervisors
     * to their own department's work orders.
     *
     * @param departmentId ID of the department
     * @return maintenance requests for that department
     */
    List<MaintenanceRequest> findByDepartmentId(Long departmentId);

    /**
     * Counts the work orders assigned to a technician
     * that have not yet been completed.
     *
     * Used by the automatic assignment algorithm
     * to identify which technician has the smallest
     * active workload.
     *
     * Completed work orders are excluded so that
     * historical assignments do not unfairly affect
     * workload distribution.
     *
     * @param technicianId ID of the technician
     * @param status status to exclude (COMPLETED)
     * @return number of active assigned work orders
     */
    long countByAssignedTechnicianIdAndStatusNot(
            Long technicianId,
            RequestStatus status
    );
}