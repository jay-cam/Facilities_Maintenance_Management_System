/**
 * Represents the current stage of a work order.
 *
 * This is separate from supervisor approval.
 *
 * OPEN -> IN_PROGRESS -> COMPLETED
 */
export type RequestStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED'

/**
 * Represents the supervisor approval state.
 *
 * PENDING_APPROVAL:
 * The request has been submitted but has not
 * yet been approved by a department supervisor.
 *
 * APPROVED:
 * A supervisor has approved the request, and
 * the backend has automatically assigned a technician.
 */
export type ApprovalStatus = 'PENDING_APPROVAL' | 'APPROVED'

/**
 * Represents the urgency of a maintenance request.
 */
export type RequestPriority = 'EMERGENCY' | 'URGENT' | 'ROUTINE'

/**
 * Represents the reported condition of the equipment.
 */
export type EquipmentCondition = 'GOOD' | 'FAIR' | 'POOR'

/**
 * Represents an operational department.
 *
 * Each requester and supervisor belongs to a department.
 */
export interface Department {
    id: number
    name: string
}

/**
 * Represents an application user.
 *
 * Roles determine which actions a user can perform.
 *
 * REQUESTER:
 * Submits and tracks maintenance requests.
 *
 * SUPERVISOR:
 * Reviews and approves requests within their department.
 *
 * TECHNICIAN:
 * Performs assigned maintenance work.
 */
export interface User {
    id: number
    name: string
    email: string
    role: 'REQUESTER' | 'TECHNICIAN' | 'SUPERVISOR'
    department: Department | null
}

/**
 * Represents a maintenance request returned by
 * the Spring Boot REST API.
 *
 * The interface mirrors the MaintenanceRequest
 * entity in the Java backend.
 *
 * Approval and work-order status are tracked
 * independently.
 */
export interface MaintenanceRequest {

    // Unique work-order identifier.
    id: number

    // Information provided when submitting a request.
    title: string
    description: string
    buildingNumber: string
    priority: RequestPriority
    condition: EquipmentCondition

    /**
     * Current work-order lifecycle state.
     *
     * OPEN, IN_PROGRESS, or COMPLETED.
     */
    status: RequestStatus

    /**
     * Current supervisor approval state.
     *
     * New requests begin as PENDING_APPROVAL.
     *
     * Once approved, the backend automatically
     * assigns the least-loaded technician.
     */
    approvalStatus: ApprovalStatus

    /**
     * Supervisor who approved the request.
     *
     * Null while approval is pending.
     *
     * May also be null for historical requests
     * created before the approval feature existed.
     */
    approvedBy: User | null

    /**
     * Date and time of supervisor approval.
     *
     * Null until approval occurs.
     *
     * Historical requests migrated to APPROVED
     * may also have a null approval timestamp.
     */
    approvedAt: string | null

    // User who submitted the request.
    requester: User

    // Department responsible for the request.
    department: Department

    /**
     * Technician assigned to the work order.
     *
     * Null until the request is approved.
     *
     * The backend selects the technician based
     * on the number of active assigned work orders.
     */
    assignedTechnician: User | null

    // Technician documentation.
    technicianNotes: string | null
    resolutionSummary: string | null

    // Work-order timestamps.
    createdAt: string
    updatedAt: string
    startedAt: string | null
    completedAt: string | null
    estimatedCompletionAt: string | null
}

/**
 * Represents the information submitted when
 * creating a new maintenance request.
 *
 * Approval status and technician assignment
 * are intentionally excluded.
 *
 * The backend automatically initializes:
 *
 * status = OPEN
 * approvalStatus = PENDING_APPROVAL
 * assignedTechnician = null
 *
 * A supervisor must approve the request before
 * the backend assigns a technician.
 */
export interface CreateMaintenanceRequest {
    title: string
    description: string
    buildingNumber: string
    priority: RequestPriority
    condition: EquipmentCondition
}