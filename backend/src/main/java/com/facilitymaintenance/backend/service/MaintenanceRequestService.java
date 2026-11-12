package com.facilitymaintenance.backend.service;

import com.facilitymaintenance.backend.dto.CreateMaintenanceRequest;
import com.facilitymaintenance.backend.model.*;
import com.facilitymaintenance.backend.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository repository;
    private final WorkOrderHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final WorkOrderCommentRepository commentRepository;

    /**
     * Constructor injection provides access to the repositories
     * responsible for work orders, users, history, and comments.
     */
    public MaintenanceRequestService(
            MaintenanceRequestRepository repository,
            WorkOrderHistoryRepository historyRepository,
            UserRepository userRepository,
            WorkOrderCommentRepository commentRepository
    ) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }


    // --------------------------------------------------
    // Shared Lookup and Access-Control Helpers
    // --------------------------------------------------

    /**
     * Retrieves a user by ID.
     *
     * Returns HTTP 404 when the user does not exist.
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                )
        );
    }

    /**
     * Retrieves a maintenance request by ID.
     *
     * Returns HTTP 404 when the request does not exist.
     */
    private MaintenanceRequest findRequest(Long requestId) {
        return repository.findById(requestId).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Maintenance request not found."
                )
        );
    }

    /**
     * Determines whether a user belongs to the same
     * department as a maintenance request.
     */
    private boolean belongsToSameDepartment(
            User user,
            MaintenanceRequest request
    ) {
        return user.getDepartment() != null
                && request.getDepartment() != null
                && user.getDepartment().getId().equals(
                request.getDepartment().getId()
        );
    }

    /**
     * Enforces department-based visibility restrictions.
     *
     * Technicians can view requests across departments.
     *
     * Requesters and supervisors can only view requests
     * belonging to their own department.
     */
    private void verifyCanViewRequest(
            User user,
            MaintenanceRequest request
    ) {

        if (user.getRole() == UserRole.TECHNICIAN) {
            return;
        }

        if ((user.getRole() == UserRole.REQUESTER
                || user.getRole() == UserRole.SUPERVISOR)
                && belongsToSameDepartment(user, request)) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You cannot access work orders outside your department."
        );
    }

    /**
     * Ensures that only the technician assigned to
     * a work order can perform technician actions.
     */
    private void verifyAssignedTechnician(
            User actor,
            MaintenanceRequest request
    ) {

        if (actor.getRole() != UserRole.TECHNICIAN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only technicians can modify work orders."
            );
        }

        if (request.getAssignedTechnician() == null
                || !request.getAssignedTechnician().getId().equals(actor.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Technician is not assigned to this work order."
            );
        }
    }


    // --------------------------------------------------
    // Create Maintenance Request
    // --------------------------------------------------

    /**
     * Creates a new maintenance request.
     *
     * New requests:
     * - Belong to the requester's department.
     * - Begin with OPEN status.
     * - Begin with PENDING_APPROVAL, approval.
     * - Have no assigned technician.
     *
     * A supervisor must approve the request before
     * it can be assigned or started.
     */
    @Transactional
    public MaintenanceRequest createRequest(
            CreateMaintenanceRequest requestData,
            Long requesterId
    ) {

        User requester = findUser(requesterId);

        if (requester.getRole() != UserRole.REQUESTER
                && requester.getRole() != UserRole.SUPERVISOR) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only requesters and supervisors can submit maintenance requests."
            );
        }

        if (requester.getDepartment() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requester must belong to a department."
            );
        }

        MaintenanceRequest request = new MaintenanceRequest(
                requestData.title(),
                requestData.description(),
                requestData.buildingNumber(),
                requestData.priority(),
                requestData.condition()
        );

        request.setRequester(requester);
        request.setDepartment(requester.getDepartment());

        request.setStatus(RequestStatus.OPEN);
        request.setApprovalStatus(ApprovalStatus.PENDING_APPROVAL);

        MaintenanceRequest savedRequest = repository.save(request);

        historyRepository.save(new WorkOrderHistory(
                savedRequest,
                requester,
                RequestStatus.OPEN,
                "Maintenance request submitted. Awaiting supervisor approval."
        ));

        return savedRequest;
    }


    // --------------------------------------------------
    // Retrieve Work Orders
    // --------------------------------------------------

    /**
     * Retrieves work orders across all departments.
     *
     * Only technicians can access this endpoint.
     */
    public List<MaintenanceRequest> getAllRequests(Long userId) {

        User user = findUser(userId);

        if (user.getRole() != UserRole.TECHNICIAN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only technicians can view all departments."
            );
        }

        return repository.findAll();
    }

    /**
     * Retrieves work orders belonging to the
     * current requester's or supervisor's department.
     */
    public List<MaintenanceRequest> getDepartmentRequests(Long userId) {

        User user = findUser(userId);

        if (user.getRole() != UserRole.REQUESTER
                && user.getRole() != UserRole.SUPERVISOR) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only requesters and supervisors can view department requests."
            );
        }

        if (user.getDepartment() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User must belong to a department."
            );
        }

        return repository.findByDepartmentId(
                user.getDepartment().getId()
        );
    }

    /**
     * Retrieves a work order after verifying that
     * the current user has permission to view it.
     */
    public MaintenanceRequest getRequestById(
            Long id,
            Long userId
    ) {

        MaintenanceRequest request = findRequest(id);
        User user = findUser(userId);

        verifyCanViewRequest(user, request);

        return request;
    }

    /**
     * Retrieves the chronological history of a work order.
     *
     * Department access restrictions also apply
     * to the history endpoint.
     */
    public List<WorkOrderHistory> getRequestHistory(
            Long requestId,
            Long userId
    ) {

        MaintenanceRequest request = findRequest(requestId);
        User user = findUser(userId);

        verifyCanViewRequest(user, request);

        return historyRepository
                .findByMaintenanceRequestIdOrderByCreatedAtAsc(requestId);
    }


    // --------------------------------------------------
    // Supervisor Approval and Automatic Assignment
    // --------------------------------------------------

    /**
     * Approves a maintenance request and immediately
     * assigns it to the technician with the fewest
     * active work orders.
     *
     * Business rules:
     *
     * 1. Only supervisors can approve requests.
     * 2. Supervisors can only approve requests
     *    belonging to their own department.
     * 3. Requests must have PENDING approval.
     * 4. Completed requests cannot be approved.
     * 5. Assignment is performed automatically.
     *
     * Workload balancing:
     *
     * Each technician's active workload is calculated
     * by counting assigned requests whose status is
     * not COMPLETED.
     *
     * The technician with the smallest count is selected.
     *
     * Ties are resolved using the lowest technician ID,
     * ensuring deterministic behavior.
     *
     * @Transactional ensures approval, assignment,
     * and history updates succeed or fail together.
     */
    @Transactional
    public MaintenanceRequest approveRequest(
            Long requestId,
            Long supervisorId
    ) {

        MaintenanceRequest request = findRequest(requestId);
        User supervisor = findUser(supervisorId);

        // Only supervisors can approve requests.
        if (supervisor.getRole() != UserRole.SUPERVISOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only supervisors can approve maintenance requests."
            );
        }

        // Supervisors are restricted to their own department.
        if (!belongsToSameDepartment(supervisor, request)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Supervisors can only approve requests in their department."
            );
        }

        // Prevent approving an already processed request.
        if (request.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This maintenance request has already been processed."
            );
        }

        // Completed requests cannot be approved.
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot approve a completed work order."
            );
        }

        // Retrieve all users with the TECHNICIAN role.
        List<User> technicians = userRepository.findByRole(
                UserRole.TECHNICIAN
        );

        if (technicians.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No technicians are available for assignment."
            );
        }

        /*
         * Select the technician with the smallest
         * number of active assigned work orders.
         *
         * Example:
         *
         * Jane:   3 active work orders
         * James:  1 active work order
         * Daniel: 2 active work orders
         *
         * James is selected because 1 is the
         * smallest active workload.
         *
         * If workloads are equal, the technician
         * with the lowest user ID is selected.
         */
        User selectedTechnician = technicians.stream()
                .min(
                        Comparator
                                .comparingLong(
                                        (User technician) ->
                                                repository.countByAssignedTechnicianIdAndStatusNot(
                                                        technician.getId(),
                                                        RequestStatus.COMPLETED
                                                )
                                )
                                .thenComparing(User::getId)
                )
                .orElseThrow();

        // Mark the request as approved.
        request.setApprovalStatus(ApprovalStatus.APPROVED);

        // Record which supervisor approved the request.
        request.setApprovedBy(supervisor);

        // Record when approval occurred.
        request.setApprovedAt(LocalDateTime.now());

        // Automatically assign the least-loaded technician.
        request.setAssignedTechnician(selectedTechnician);

        // Save the approved request and technician assignment.
        MaintenanceRequest savedRequest = repository.save(request);

        // Record the approval and assignment for auditing.
        historyRepository.save(new WorkOrderHistory(
                savedRequest,
                supervisor,
                savedRequest.getStatus(),
                "Request approved. Automatically assigned to "
                        + selectedTechnician.getName()
                        + " based on active workload."
        ));

        return savedRequest;
    }


    // --------------------------------------------------
    // Update Work-Order Status
    // --------------------------------------------------

    /**
     * Allows the assigned technician to start
     * an approved work order.
     *
     * Valid transition:
     *
     * OPEN -> IN_PROGRESS
     */
    @Transactional
    public MaintenanceRequest updateRequestStatus(
            Long id,
            RequestStatus status,
            Long actorId
    ) {

        MaintenanceRequest request = findRequest(id);
        User actor = findUser(actorId);

        verifyAssignedTechnician(actor, request);

        // Work orders must be approved before work begins.
        if (request.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Work order must be approved before it can be started."
            );
        }

        if (request.getStatus() != RequestStatus.OPEN
                || status != RequestStatus.IN_PROGRESS) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid status transition. Use the completion endpoint to finish a work order."
            );
        }

        request.setStatus(RequestStatus.IN_PROGRESS);
        request.setStartedAt(LocalDateTime.now());

        MaintenanceRequest savedRequest = repository.save(request);

        historyRepository.save(new WorkOrderHistory(
                savedRequest,
                actor,
                RequestStatus.IN_PROGRESS,
                "Work order started."
        ));

        return savedRequest;
    }


    // --------------------------------------------------
    // Add Technician Note
    // --------------------------------------------------

    /**
     * Allows the assigned technician to add
     * notes to an active work order.
     */
    @Transactional
    public MaintenanceRequest addTechnicianNote(
            Long requestId,
            String note,
            Long actorId
    ) {

        MaintenanceRequest request = findRequest(requestId);
        User actor = findUser(actorId);

        verifyAssignedTechnician(actor, request);

        if (request.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Work order must be approved before technician updates."
            );
        }

        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot add notes to a completed work order."
            );
        }

        if (note == null || note.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Technician note cannot be empty."
            );
        }

        request.setTechnicianNotes(note);

        MaintenanceRequest savedRequest = repository.save(request);

        historyRepository.save(new WorkOrderHistory(
                savedRequest,
                actor,
                savedRequest.getStatus(),
                note
        ));

        return savedRequest;
    }


    // --------------------------------------------------
    // Update Estimated Completion
    // --------------------------------------------------

    /**
     * Allows the assigned technician to provide
     * an estimated completion date and time.
     *
     * The estimate must be in the future.
     */
    @Transactional
    public MaintenanceRequest updateEstimatedCompletion(
            Long requestId,
            LocalDateTime estimatedCompletionAt,
            Long actorId
    ) {

        MaintenanceRequest request = findRequest(requestId);
        User actor = findUser(actorId);

        verifyAssignedTechnician(actor, request);

        if (request.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Work order must be approved before technician updates."
            );
        }

        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot update the estimated completion date of a completed work order."
            );
        }

        if (estimatedCompletionAt == null
                || !estimatedCompletionAt.isAfter(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estimated completion must be a future date and time."
            );
        }

        request.setEstimatedCompletionAt(estimatedCompletionAt);

        MaintenanceRequest savedRequest = repository.save(request);

        historyRepository.save(new WorkOrderHistory(
                savedRequest,
                actor,
                savedRequest.getStatus(),
                "Estimated completion updated to " + estimatedCompletionAt
        ));

        return savedRequest;
    }


    // --------------------------------------------------
    // Complete Work Order
    // --------------------------------------------------

    /**
     * Allows the assigned technician to complete
     * an approved work order.
     *
     * Requirements:
     *
     * - Request must be approved.
     * - Status must be IN_PROGRESS.
     * - A resolution summary must be provided.
     */
    @Transactional
    public MaintenanceRequest completeRequest(
            Long requestId,
            String resolutionSummary,
            Long actorId
    ) {

        MaintenanceRequest request = findRequest(requestId);
        User actor = findUser(actorId);

        verifyAssignedTechnician(actor, request);

        if (request.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Work order must be approved before completion."
            );
        }

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only work orders in progress can be completed."
            );
        }

        if (resolutionSummary == null || resolutionSummary.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A resolution summary is required."
            );
        }

        request.setStatus(RequestStatus.COMPLETED);
        request.setResolutionSummary(resolutionSummary.trim());
        request.setCompletedAt(LocalDateTime.now());

        MaintenanceRequest savedRequest = repository.save(request);

        historyRepository.save(new WorkOrderHistory(
                savedRequest,
                actor,
                RequestStatus.COMPLETED,
                "Work order completed. Resolution: "
                        + resolutionSummary.trim()
        ));

        return savedRequest;
    }


    // --------------------------------------------------
    // Comments
    // --------------------------------------------------

    /**
     * Adds a comment to a work order.
     *
     * Users must have permission to view the request.
     *
     * Comments remain available throughout the
     * approval and maintenance workflow.
     */
    @Transactional
    public WorkOrderComment addComment(
            Long requestId,
            Long authorId,
            String content
    ) {

        MaintenanceRequest request = findRequest(requestId);
        User author = findUser(authorId);

        verifyCanViewRequest(author, request);

        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comment cannot be empty."
            );
        }

        return commentRepository.save(new WorkOrderComment(
                request,
                author,
                content.trim()
        ));
    }

    /**
     * Retrieves comments belonging to a work order.
     *
     * Department visibility restrictions apply.
     */
    public List<WorkOrderComment> getRequestComments(
            Long requestId,
            Long userId
    ) {

        MaintenanceRequest request = findRequest(requestId);
        User user = findUser(userId);

        verifyCanViewRequest(user, request);

        return commentRepository
                .findByMaintenanceRequestIdOrderByCreatedAtAsc(requestId);
    }
}