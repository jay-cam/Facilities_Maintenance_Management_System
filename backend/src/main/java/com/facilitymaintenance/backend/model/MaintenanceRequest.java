package com.facilitymaintenance.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a maintenance request submitted by a user.
 *
 * This entity maps to the maintenance_requests table
 * in PostgreSQL.
 *
 * A maintenance request has two independent states:
 *
 * 1. Approval state:
 *    PENDING_APPROVAL -> APPROVED
 *
 * 2. Work-order state:
 *    OPEN -> IN_PROGRESS -> COMPLETED
 *
 * The separation allows supervisors to approve work
 * without directly assigning technicians or changing
 * the work-order lifecycle.
 */
@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {

    // --------------------------------------------------
    // Primary Key
    // --------------------------------------------------

    /**
     * Unique database identifier for the work order.
     *
     * PostgreSQL generates this value automatically.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // --------------------------------------------------
    // Request Information
    // --------------------------------------------------

    /**
     * Short description identifying the maintenance issue.
     */
    @Column(nullable = false, length = 150)
    private String title;

    /**
     * Detailed description of the maintenance problem.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Building number associated with the request.
     */
    @Column(name = "building_number", nullable = false, length = 50)
    private String buildingNumber;

    /**
     * Indicates the urgency of the maintenance request.
     *
     * Possible values:
     * ROUTINE, URGENT, EMERGENCY.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestPriority priority;

    /**
     * Represents the current work-order lifecycle state.
     *
     * Possible values:
     * OPEN, IN_PROGRESS, COMPLETED.
     *
     * This is separate from supervisor approval.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    /**
     * Building or equipment condition selected
     * when the request was created.
     *
     * Possible values:
     * GOOD, FAIR, POOR.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_condition", nullable = false)
    private EquipmentCondition condition;


    // --------------------------------------------------
    // Supervisor Approval
    // --------------------------------------------------

    /**
     * Tracks whether a supervisor has approved the request.
     *
     * New requests begin as PENDING_APPROVAL.
     *
     * Once approved, the backend automatically assigns
     * the work order to the least-loaded technician.
     *
     * The column is initially nullable to preserve
     * compatibility with existing database records.
     *
     * Existing records will be migrated to APPROVED
     * before enforcing a NOT NULL database constraint.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING_APPROVAL;

    /**
     * References the supervisor who approved the request.
     *
     * Remains null until the request is approved.
     */
    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    /**
     * Records when the supervisor approved the request.
     *
     * Remains null while approval is pending.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;


    // --------------------------------------------------
    // User and Department Relationships
    // --------------------------------------------------

    /**
     * User who originally submitted the request.
     */
    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester;

    /**
     * Department responsible for the request.
     *
     * Supervisors and requesters can only access
     * requests belonging to their own department.
     */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    /**
     * Technician assigned to perform the work.
     *
     * Assignment occurs automatically after
     * supervisor approval.
     */
    @ManyToOne
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician;


    // --------------------------------------------------
    // Work Order Dates
    // --------------------------------------------------

    /**
     * Date and time the request was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date and time the request was last modified.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Date and time the assigned technician
     * started working on the request.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Date and time the technician completed the work.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Estimated date and time for completion.
     */
    @Column(name = "estimated_completion_at")
    private LocalDateTime estimatedCompletionAt;


    // --------------------------------------------------
    // Technician Documentation
    // --------------------------------------------------

    /**
     * Notes entered by the assigned technician.
     */
    @Column(name = "technician_notes", columnDefinition = "TEXT")
    private String technicianNotes;

    /**
     * Summary describing how the maintenance
     * issue was resolved.
     */
    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;


    // --------------------------------------------------
    // Constructors
    // --------------------------------------------------

    /**
     * Required by JPA/Hibernate when retrieving
     * and creating entity instances.
     */
    public MaintenanceRequest() {
    }

    /**
     * Creates a new maintenance request.
     *
     * New requests begin:
     *
     * Work-order status: OPEN
     * Approval status: PENDING_APPROVAL
     *
     * The supervisor must approve the request
     * before the system assigns a technician.
     */
    public MaintenanceRequest(
            String title,
            String description,
            String buildingNumber,
            RequestPriority priority,
            EquipmentCondition condition
    ) {
        this.title = title;
        this.description = description;
        this.buildingNumber = buildingNumber;
        this.priority = priority;
        this.condition = condition;

        this.status = RequestStatus.OPEN;
        this.approvalStatus = ApprovalStatus.PENDING_APPROVAL;
    }


    // --------------------------------------------------
    // Getters
    // --------------------------------------------------

    /**
     * Getters provide read access to entity fields.
     *
     * Spring Boot uses these methods when serializing
     * maintenance requests into JSON responses.
     */
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

    public EquipmentCondition getCondition() {
        return condition;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public User getRequester() {
        return requester;
    }

    public Department getDepartment() {
        return department;
    }

    public User getAssignedTechnician() {
        return assignedTechnician;
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

    public LocalDateTime getEstimatedCompletionAt() {
        return estimatedCompletionAt;
    }

    public String getTechnicianNotes() {
        return technicianNotes;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }


    // --------------------------------------------------
    // Setters
    // --------------------------------------------------

    /**
     * Setters allow the service layer to update
     * maintenance request information.
     *
     * Business rules governing these updates
     * are enforced by MaintenanceRequestService.
     */
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

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setAssignedTechnician(User assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setEstimatedCompletionAt(
            LocalDateTime estimatedCompletionAt
    ) {
        this.estimatedCompletionAt = estimatedCompletionAt;
    }

    public void setTechnicianNotes(String technicianNotes) {
        this.technicianNotes = technicianNotes;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
    }


    // --------------------------------------------------
    // JPA Lifecycle Callbacks
    // --------------------------------------------------

    /**
     * Runs automatically before Hibernate inserts
     * a new maintenance request into PostgreSQL.
     *
     * Initializes the creation and update timestamps.
     *
     * Also ensures newly created requests have
     * a pending approval state.
     */
    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.approvalStatus == null) {
            this.approvalStatus = ApprovalStatus.PENDING_APPROVAL;
        }
    }

    /**
     * Runs automatically before Hibernate updates
     * an existing maintenance request.
     *
     * Automatically refreshes the updatedAt timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}