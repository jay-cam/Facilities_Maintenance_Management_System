package com.facilitymaintenance.backend.model;

import jakarta.persistence.*;
import org.springframework.jmx.export.annotation.ManagedNotification;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public RequestPriority getPriority() {
        return priority;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public User getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(User assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "building_number", nullable = false, length = 50)
    private String buildingNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public LocalDateTime getEstimatedCompletionAt() {
        return estimatedCompletionAt;
    }

    public void setEstimatedCompletionAt(LocalDateTime estimatedCompletionAt) {
        this.estimatedCompletionAt = estimatedCompletionAt;
    }

    @Column(name = "estimated_completion_at")
    private LocalDateTime estimatedCompletionAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_condition", nullable = false)
    private EquipmentCondition condition;

    @Column(name = "technician_notes", columnDefinition = "Text")
    private String technicianNotes;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
    }

    public String getTechnicianNotes() {
        return technicianNotes;
    }

    public void setTechnicianNotes(String technicianNotes) {
        this.technicianNotes = technicianNotes;
    }

    public void setCompletedAt(LocalDateTime completedAt){
        this.completedAt = completedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public MaintenanceRequest(){

    }

    public MaintenanceRequest(String title, String description,
                              String buildingNumber, RequestPriority priority, EquipmentCondition condition){
        this.title = title;
        this.description = description;
        this.buildingNumber = buildingNumber;
        this.priority = priority;
        this.condition = condition;
        this.status = RequestStatus.OPEN;
    }

    public EquipmentCondition getCondition() {
        return condition;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public void setCondition(EquipmentCondition condition) {
        this.condition = condition;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private  User requester;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @ManyToOne
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician;

    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
