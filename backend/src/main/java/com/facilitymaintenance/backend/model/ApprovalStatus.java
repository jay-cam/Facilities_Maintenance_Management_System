package com.facilitymaintenance.backend.model;

/**
 * Represents the approval stage of a maintenance request.
 *
 * Approval is tracked separately from the work-order
 * lifecycle (OPEN, IN_PROGRESS, COMPLETED).
 *
 * PENDING_APPROVAL:
 * The request has been submitted but has not yet
 * been approved by a department supervisor.
 *
 * APPROVED:
 * A supervisor has approved the request, allowing
 * the system to assign a technician automatically.
 */
public enum ApprovalStatus {
    PENDING_APPROVAL,
    APPROVED
}