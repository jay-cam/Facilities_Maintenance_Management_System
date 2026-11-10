package com.facilitymaintenance.backend.controller;

import com.facilitymaintenance.backend.model.MaintenanceRequest;
import com.facilitymaintenance.backend.service.MaintenanceRequestService;
import com.facilitymaintenance.backend.model.RequestStatus;
import com.facilitymaintenance.backend.model.WorkOrderHistory;
import com.facilitymaintenance.backend.dto.CreateMaintenanceRequest;
import com.facilitymaintenance.backend.model.WorkOrderComment;

import com.sun.tools.javac.Main;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requests")
public class MaintenanceRequestController {

    private final MaintenanceRequestService service;

    public MaintenanceRequestController(MaintenanceRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<MaintenanceRequest> getAllRequests(){
        return service.getAllRequests();
    }

    @GetMapping("/department")
    public List<MaintenanceRequest> getDepartmentRequests(
            @RequestParam Long userId) {

        return service.getDepartmentRequests(userId);
    }

    @GetMapping("/{id}")
    public MaintenanceRequest getRequestById (@PathVariable Long id){
        return service.getRequestById(id);
    }

    @GetMapping("/{id}/history")
    public List<WorkOrderHistory> getRequestHistory(@PathVariable Long id) {
        return service.getRequestHistory(id);
    }

    @PostMapping
    public MaintenanceRequest createRequest(@Valid @RequestBody CreateMaintenanceRequest requestData,
                                            @RequestParam Long requesterId){
        return service.createRequest(requestData, requesterId);
    }

    @PatchMapping("/{id}/status")
    public MaintenanceRequest updateRequestStatus(@PathVariable Long id,
                                                  @RequestBody RequestStatus status, @RequestParam Long actorId) {
        return service.updateRequestStatus(id, status, actorId);
    }

    @PatchMapping("/{id}/assign")
    public MaintenanceRequest assignTechnician(
            @PathVariable Long id,
            @RequestParam Long technicianId,
            @RequestParam Long supervisorId) {

        return service.assignTechnician(id, technicianId, supervisorId);
    }

    @PatchMapping("/{id}/notes")
    public MaintenanceRequest addTechnicianNote(
            @PathVariable Long id,
            @RequestBody String note,
            @RequestParam Long actorId) {
        return service.addTechnicianNote(id, note, actorId);
    }

    @PatchMapping("/{id}/estimated-completion")
    public MaintenanceRequest updateEstimatedCompletion(
            @PathVariable Long id,
            @RequestBody LocalDateTime estimatedCompletionAt,
            @RequestParam Long actorId) {

        return service.updateEstimatedCompletion(
                id,
                estimatedCompletionAt,
                actorId
        );
    }

    @PatchMapping("/{id}/complete")
    public MaintenanceRequest completeRequest(
            @PathVariable Long id,
            @RequestBody String resolutionSummary,
            @RequestParam Long actorId) {

        return service.completeRequest(
                id,
                resolutionSummary,
                actorId
        );
    }

    @PostMapping("/{id}/comments")
    public WorkOrderComment addComment(
            @PathVariable Long id,
            @RequestParam Long authorId,
            @RequestBody String content) {

        return service.addComment(id, authorId, content);
    }

    @GetMapping("/{id}/comments")
    public List<WorkOrderComment> getRequestComments(
            @PathVariable Long id,
            @RequestParam Long userId) {

        return service.getRequestComments(id, userId);
    }
}
