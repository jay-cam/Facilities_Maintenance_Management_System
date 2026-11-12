package com.facilitymaintenance.backend.controller;

import com.facilitymaintenance.backend.dto.CreateMaintenanceRequest;
import com.facilitymaintenance.backend.model.MaintenanceRequest;
import com.facilitymaintenance.backend.model.RequestStatus;
import com.facilitymaintenance.backend.model.WorkOrderComment;
import com.facilitymaintenance.backend.model.WorkOrderHistory;
import com.facilitymaintenance.backend.service.MaintenanceRequestService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller responsible for exposing maintenance
 * request operations through HTTP endpoints.
 *
 * The controller receives incoming HTTP requests and
 * delegates business logic to MaintenanceRequestService.
 *
 * Responsibilities:
 *
 * - Map HTTP requests to Java methods.
 * - Extract path variables and request parameters.
 * - Validate incoming request data.
 * - Return service results as JSON responses.
 *
 * Business rules, department restrictions, supervisor
 * approval, and technician assignment are handled
 * by the service layer rather than the controller.
 */
@RestController
@RequestMapping("/api/requests")
public class MaintenanceRequestController {

    private final MaintenanceRequestService service;

    /**
     * Constructor injection provides access to
     * the maintenance request service.
     */
    public MaintenanceRequestController(
            MaintenanceRequestService service
    ) {
        this.service = service;
    }


    // --------------------------------------------------
    // Retrieve Work Orders
    // --------------------------------------------------

    /**
     * GET /api/requests?userId=2
     *
     * Retrieves maintenance requests across all departments.
     *
     * Only technicians are permitted to use this endpoint.
     *
     * The service verifies the user's role before
     * retrieving the work orders.
     */
    @GetMapping
    public List<MaintenanceRequest> getAllRequests(
            @RequestParam Long userId
    ) {
        return service.getAllRequests(userId);
    }

    /**
     * GET /api/requests/department?userId=1
     *
     * Retrieves maintenance requests belonging
     * to the specified user's department.
     *
     * Intended for requesters and supervisors.
     *
     * Department restrictions are enforced
     * by the service layer.
     */
    @GetMapping("/department")
    public List<MaintenanceRequest> getDepartmentRequests(
            @RequestParam Long userId
    ) {
        return service.getDepartmentRequests(userId);
    }

    /**
     * GET /api/requests/{id}?userId=1
     *
     * Retrieves a specific maintenance request.
     *
     * The service verifies that the user has
     * permission to view the work order.
     */
    @GetMapping("/{id}")
    public MaintenanceRequest getRequestById(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        return service.getRequestById(id, userId);
    }

    /**
     * GET /api/requests/{id}/history?userId=1
     *
     * Retrieves the chronological history
     * of a maintenance request.
     *
     * Includes events such as:
     *
     * - Request submission.
     * - Supervisor approval.
     * - Automatic technician assignment.
     * - Work-order status changes.
     * - Technician notes.
     * - Completion.
     *
     * The service enforces department-based
     * visibility restrictions.
     */
    @GetMapping("/{id}/history")
    public List<WorkOrderHistory> getRequestHistory(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        return service.getRequestHistory(id, userId);
    }


    // --------------------------------------------------
    // Create Maintenance Request
    // --------------------------------------------------

    /**
     * POST /api/requests?requesterId=1
     *
     * Creates a new maintenance request.
     *
     * @Valid triggers validation rules defined
     * in the CreateMaintenanceRequest DTO.
     *
     * The service automatically:
     *
     * - Associates the request with the user's department.
     * - Sets its initial status to OPEN.
     * - Sets approval status to PENDING.
     * - Records the submission in work-order history.
     *
     * The request remains unassigned until
     * a supervisor approves it.
     */
    @PostMapping
    public MaintenanceRequest createRequest(
            @Valid @RequestBody CreateMaintenanceRequest requestData,
            @RequestParam Long requesterId
    ) {
        return service.createRequest(requestData, requesterId);
    }


    // --------------------------------------------------
    // Supervisor Operations
    // --------------------------------------------------

    /**
     * PATCH /api/requests/{id}/approve?supervisorId=3
     *
     * Approves a maintenance request.
     *
     * Only supervisors belonging to the request's
     * department can approve it.
     *
     * Once approved, the service automatically:
     *
     * 1. Retrieves all technicians.
     * 2. Counts each technician's active work orders.
     * 3. Selects the technician with the fewest assignments.
     * 4. Uses the lowest technician ID to break ties.
     * 5. Assigns the selected technician.
     * 6. Records the approval and assignment in history.
     *
     * The controller does not perform these operations
     * itself; they belong to the service layer.
     */
    @PatchMapping("/{id}/approve")
    public MaintenanceRequest approveRequest(
            @PathVariable Long id,
            @RequestParam Long supervisorId
    ) {
        return service.approveRequest(id, supervisorId);
    }


    // --------------------------------------------------
    // Technician Operations
    // --------------------------------------------------

    /**
     * PATCH /api/requests/{id}/status?actorId=2
     *
     * Allows an assigned technician to start
     * an approved maintenance request.
     *
     * Expected request body:
     *
     * "IN_PROGRESS"
     *
     * The service validates the status transition
     * and records when work begins.
     */
    @PatchMapping("/{id}/status")
    public MaintenanceRequest updateRequestStatus(
            @PathVariable Long id,
            @RequestBody RequestStatus status,
            @RequestParam Long actorId
    ) {
        return service.updateRequestStatus(id, status, actorId);
    }

    /**
     * PATCH /api/requests/{id}/notes?actorId=2
     *
     * Allows the assigned technician to add
     * notes to an approved work order.
     *
     * The service validates technician permissions
     * and records the note in work-order history.
     */
    @PatchMapping("/{id}/notes")
    public MaintenanceRequest addTechnicianNote(
            @PathVariable Long id,
            @RequestBody String note,
            @RequestParam Long actorId
    ) {
        return service.addTechnicianNote(id, note, actorId);
    }

    /**
     * PATCH /api/requests/{id}/estimated-completion?actorId=2
     *
     * Allows the assigned technician to update
     * the estimated completion date and time.
     *
     * The service ensures that:
     *
     * - The technician is assigned to the request.
     * - The request has been approved.
     * - The request has not been completed.
     * - The estimated completion is in the future.
     */
    @PatchMapping("/{id}/estimated-completion")
    public MaintenanceRequest updateEstimatedCompletion(
            @PathVariable Long id,
            @RequestBody LocalDateTime estimatedCompletionAt,
            @RequestParam Long actorId
    ) {
        return service.updateEstimatedCompletion(
                id,
                estimatedCompletionAt,
                actorId
        );
    }

    /**
     * PATCH /api/requests/{id}/complete?actorId=2
     *
     * Completes an approved work order.
     *
     * The request must already be IN_PROGRESS.
     *
     * A resolution summary is required.
     *
     * The service records the completion timestamp
     * and adds a corresponding history entry.
     */
    @PatchMapping("/{id}/complete")
    public MaintenanceRequest completeRequest(
            @PathVariable Long id,
            @RequestBody String resolutionSummary,
            @RequestParam Long actorId
    ) {
        return service.completeRequest(
                id,
                resolutionSummary,
                actorId
        );
    }


    // --------------------------------------------------
    // Work-Order Comments
    // --------------------------------------------------

    /**
     * POST /api/requests/{id}/comments?authorId=1
     *
     * Adds a comment to a maintenance request.
     *
     * Comments allow requesters, supervisors,
     * and technicians to communicate about
     * maintenance issues.
     *
     * The service verifies that the author
     * has permission to view the request.
     */
    @PostMapping("/{id}/comments")
    public WorkOrderComment addComment(
            @PathVariable Long id,
            @RequestParam Long authorId,
            @RequestBody String content
    ) {
        return service.addComment(
                id,
                authorId,
                content
        );
    }

    /**
     * GET /api/requests/{id}/comments?userId=1
     *
     * Retrieves comments associated with
     * a maintenance request.
     *
     * Results are returned in chronological order.
     *
     * The service verifies department access
     * before returning the comments.
     */
    @GetMapping("/{id}/comments")
    public List<WorkOrderComment> getRequestComments(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        return service.getRequestComments(
                id,
                userId
        );
    }
}