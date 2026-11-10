package com.facilitymaintenance.backend.service;

import com.facilitymaintenance.backend.model.MaintenanceRequest;
import com.facilitymaintenance.backend.model.RequestStatus;
import com.facilitymaintenance.backend.repository.MaintenanceRequestRepository;
import com.facilitymaintenance.backend.model.WorkOrderHistory;
import com.facilitymaintenance.backend.model.User;
import com.facilitymaintenance.backend.repository.WorkOrderCommentRepository;
import com.facilitymaintenance.backend.repository.WorkOrderHistoryRepository;
import com.facilitymaintenance.backend.repository.UserRepository;
import com.facilitymaintenance.backend.model.UserRole;
import com.facilitymaintenance.backend.dto.CreateMaintenanceRequest;
import com.facilitymaintenance.backend.repository.WorkOrderCommentRepository;
import com.facilitymaintenance.backend.model.WorkOrderComment;

import com.sun.tools.javac.Main;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.net.ssl.HttpsURLConnection;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository repository;
    private final WorkOrderHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final WorkOrderCommentRepository commentRepository;

    public MaintenanceRequestService(MaintenanceRequestRepository repository,
                                     WorkOrderHistoryRepository historyRepository,
                                     UserRepository userRepository, WorkOrderCommentRepository commentRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public MaintenanceRequest createRequest(CreateMaintenanceRequest requestData, Long requesterId) {

        // Find the user submitting the maintenance request.
        User requester = userRepository.findById(requesterId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Requester not found."
        ));

        // Verify the user has the REQUESTER role.
        if(requester.getRole() != UserRole.REQUESTER && requester.getRole() != UserRole.SUPERVISOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only requesters and supervisors can submit maintenance requests."
            );
        }

        // Construct a new entity using only approved fields.
        MaintenanceRequest request = new MaintenanceRequest(
                requestData.title(),
                requestData.description(),
                requestData.buildingNumber(),
                requestData.priority(),
                requestData.condition()
        );

        // Associate the request with the submitting user.
        request.setRequester(requester);

        // Verify the requester belongs to a department
        if(requester.getDepartment() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requester must belong to a department."
            );
        }

        // Automatically assign the requester's department.
        request.setDepartment(requester.getDepartment());

        // New maintenance requests always begin OPEN.
        request.setStatus(RequestStatus.OPEN);

        // Save the maintenance request.
        MaintenanceRequest savedRequest = repository.save(request);

        // Record the submission in work-order history.
        historyRepository.save(new WorkOrderHistory(
                savedRequest,
                requester,
                RequestStatus.OPEN,
                "Maintenance request submitted."
        ));

        return savedRequest;
    }

    public List<MaintenanceRequest> getAllRequests() {
        return repository.findAll();
    }

    public List<MaintenanceRequest> getDepartmentRequests(Long userId) {

        // Find the user.
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        // Only requesters and supervisors can view department requests
        if (user.getRole() != UserRole.REQUESTER && user.getRole() != UserRole.SUPERVISOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only requesters and supervisors can view department requests."
            );
        }

            // Verify the user belongs to a department.
            if(user.getDepartment() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "User must belong to a department."
                );
            }

            // Retrieve all maintenance requests belonging to the user's department.
            return repository.findByDepartmentId(
                    user.getDepartment().getId()
            );
        }

    public List<WorkOrderHistory> getRequestHistory(Long requestId){

        if(!repository.existsById(requestId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Maintenance request not found."
            );
        }

        return historyRepository.findByMaintenanceRequestIdOrderByCreatedAtAsc(requestId);
    }

    public MaintenanceRequest getRequestById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Maintenance request not found."
        ));
    }

    @Transactional
    public MaintenanceRequest updateRequestStatus(Long id, RequestStatus status, Long actorId) {

        MaintenanceRequest request = repository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Maintenance request not found."
                ));

        User actor = userRepository.findById(actorId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        // Only technicians can update work-order status.
        if (actor.getRole() != UserRole.TECHNICIAN){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only technicians can update work order status."
            );
        }

        // Technician must be assigned to this work order.
        if (request.getAssignedTechnician() == null || !request.getAssignedTechnician().getId().equals(actor.getId())){

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Technician is not assigned to this work order."
            );
        }

        // Only allow OPEN -> IN_PROGRESS.
        if(request.getStatus() != RequestStatus.OPEN || status != RequestStatus.IN_PROGRESS) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid status transition. Use the completion endpoint to finish a work order."
            );
        }

        // Update status and record when work began.
        request.setStatus(RequestStatus.IN_PROGRESS);
        request.setStartedAt(LocalDateTime.now());

        MaintenanceRequest savedRequest = repository.save(request);

        // Record the status change in work-order history.
        historyRepository.save(new WorkOrderHistory(
                savedRequest,
                actor,
                RequestStatus.IN_PROGRESS,
                "Work order started."
        ));

        return savedRequest;
    }

    @Transactional
    public MaintenanceRequest assignTechnician(
            Long requestId,
            Long technicianId,
            Long supervisorId) {
        MaintenanceRequest request = repository.findById(requestId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Maintenance request not found."
        ));

        // Prevent assigning technicians to completed work orders.
        if( request.getStatus() == RequestStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot assign technicians to a completed work order."
            );
        }

        User technician = userRepository.findById(technicianId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Technician not found."
        ));

        User supervisor = userRepository.findById(supervisorId).orElseThrow(()-> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Supervisor not found."
        ));

        if (supervisor.getRole() != UserRole.SUPERVISOR){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only supervisors can assign technicians."
            );
        }

        if (technician.getRole() != UserRole.TECHNICIAN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected user is not a technician."
            );
        }

        request.setAssignedTechnician(technician);

        MaintenanceRequest savedRequest = repository.save(request);

        WorkOrderHistory history = new WorkOrderHistory(
                savedRequest,
                supervisor,
                savedRequest.getStatus(),
                "Assigned technician: " + technician.getName()
        );

        historyRepository.save(history);

        return savedRequest;
    }

    @Transactional
    public MaintenanceRequest addTechnicianNote(Long requestId, String note, Long actorId) {

        MaintenanceRequest request = repository.findById(requestId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Maintenance request not found."
        ));

        User actor = userRepository.findById(actorId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        if (actor.getRole() != UserRole.TECHNICIAN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only technicians can add work order notes."
            );
        }

        if (request.getAssignedTechnician() == null || !request.getAssignedTechnician().getId().equals(actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Technician is not assigned to this work order."
            );
        }

        // Prevent modification to completed work orders.
        if(request.getStatus() == RequestStatus.COMPLETED) {
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

        MaintenanceRequest savedRequest = repository.save((request));

        WorkOrderHistory history = new WorkOrderHistory(
                savedRequest,
                actor,
                savedRequest.getStatus(),
                note
        );
        historyRepository.save(history);

        return savedRequest;
    }

    @Transactional
    public MaintenanceRequest updateEstimatedCompletion(Long requestId,
                                                        LocalDateTime estimatedCompletionAt,
                                                        Long actorId) {
        MaintenanceRequest request = repository.findById(requestId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Maintenance request not found."
        ));

        User actor = userRepository.findById((actorId)).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        // Verify the user is a technician.
        if (actor.getRole() != UserRole.TECHNICIAN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only technicians can update estimated completion times."
            );
        }

        // Verify the technician is assigned to this request.
        if (request.getAssignedTechnician() == null || !request.getAssignedTechnician().getId().equals(actor.getId())){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Technician is not assigned to this work order."
            );
        }

        // Prevent changes to estimated completion dates after completion.
        if(request.getStatus() == RequestStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot update the estimated completion date of a completed work order."
            );
        }

        // Validate the estimated completion time.
        if (estimatedCompletionAt == null || !estimatedCompletionAt.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estimated completion must be a future date and time."
            );
        }

        // Update the maintenance request.
        request.setEstimatedCompletionAt(estimatedCompletionAt);

        MaintenanceRequest savedRequest = repository.save(request);

        // Record the change in work-order history.
        WorkOrderHistory history = new WorkOrderHistory(
                savedRequest,
                actor,
                savedRequest.getStatus(),
                "Estimated completion updated to " + estimatedCompletionAt
        );
        historyRepository.save(history);

        return savedRequest;
    }

    @Transactional
    public MaintenanceRequest completeRequest(
            Long requestId,
            String resolutionSummary,
            Long actorId) {

        MaintenanceRequest request = repository.findById(requestId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Maintenance request not found."
        ));

        User actor = userRepository.findById(actorId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        // Verify that the user is a technician.
        if(actor.getRole() != UserRole.TECHNICIAN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only technicians can complete work orders."
            );
        }

        // Verify that the technician is assigned to this work order.
        if (request.getAssignedTechnician() == null || ! request.getAssignedTechnician().getId().equals(actor.getId())){

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Technician is not assigned to this work order."
            );
        }

        // A work order must be in progress before it can be completed.
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only work order in progress can be completed."
            );
        }

        // Require a meaningful resolution summary.
        if (resolutionSummary == null || resolutionSummary.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A resolution summary is required."
            );
        }

        // Record completion details.
        request.setStatus(RequestStatus.COMPLETED);
        request.setResolutionSummary(resolutionSummary.trim());
        request.setCompletedAt(LocalDateTime.now());

        MaintenanceRequest savedRequest = repository.save(request);

        // Record the completion in work-order history.
        WorkOrderHistory history = new WorkOrderHistory(
                savedRequest,
                actor,
                RequestStatus.COMPLETED,
                "Work order completed. Resolution: " + resolutionSummary.trim()
        );

        historyRepository.save(history);

        return savedRequest;
    }

    @Transactional
    public WorkOrderComment addComment(Long requestId, Long authorId, String content) {

        // Find the maintenance request.
        MaintenanceRequest request = repository.findById(requestId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Maintenance request not found."
        ));

        // Find the user submitting the comment.
        User author = userRepository.findById(authorId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        // Reject empty comments.
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comment cannot be empty."
            );
        }

        // Requesters and supervisors can only comment within their department.
        if (author.getRole() == UserRole.REQUESTER || author.getRole() == UserRole.SUPERVISOR){

            if (author.getDepartment() == null ||
            request.getDepartment() == null ||
            !author.getDepartment().getId().equals(request.getDepartment().getId())) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only comment on work orders within your department."
                );
            }
        }

        // Create and save the comment.
        WorkOrderComment comment = new WorkOrderComment(
                request,
                author,
                content.trim()
        );

        return commentRepository.save(comment);
    }

    public List<WorkOrderComment> getRequestComments(Long requestId, Long userId) {

        // Find the maintenance request.
        MaintenanceRequest request = repository.findById(requestId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Maintenance request not found."
        ));

        // Find the user requesting the comments.
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found."
        ));

        // Requesters and supervisors can only view comments within their department.
        if (user.getRole() == UserRole.REQUESTER || user.getRole() == UserRole.SUPERVISOR) {

            if(user.getDepartment() == null || request.getDepartment() == null ||
            !user.getDepartment().getId().equals((request.getDepartment().getId()))) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only view comments on work orders within your department."
                );
            }
        }

        // Retrieve comments in chronological order
        return commentRepository.findByMaintenanceRequestIdOrderByCreatedAtAsc(requestId);
    }
}
