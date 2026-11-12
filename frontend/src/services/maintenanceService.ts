import { apiRequest } from './api'

import type {
    MaintenanceRequest,
    CreateMaintenanceRequest,
    RequestStatus,
} from '../types/maintenance'


// --------------------------------------------------
// Retrieve Maintenance Requests
// --------------------------------------------------

/**
 * Retrieves maintenance requests based on the user's role.
 *
 * Technicians can view requests across all departments.
 * Requesters and supervisors can only view requests
 * belonging to their assigned department.
 *
 * The backend enforces these restrictions.
 *
 * @param userId - ID of the currently selected demo user.
 * @param role - Role of the current user.
 * @returns A list of maintenance requests.
 */
export async function getMaintenanceRequests(
    userId: number,
    role: string
): Promise<MaintenanceRequest[]> {

    // Technicians can retrieve work orders across all departments.
    if (role === 'TECHNICIAN') {
        return apiRequest<MaintenanceRequest[]>(
            `/requests?userId=${userId}`
        )
    }

    // Requesters and supervisors receive department-specific work orders.
    return apiRequest<MaintenanceRequest[]>(
        `/requests/department?userId=${userId}`
    )
}


// --------------------------------------------------
// Retrieve Individual Maintenance Request
// --------------------------------------------------

/**
 * Retrieves the details of a specific maintenance request.
 *
 * The userId is sent to the backend so it can verify
 * whether the user is permitted to view the work order.
 *
 * Requesters and supervisors are restricted to their
 * department, while technicians can view all departments.
 *
 * @param requestId - ID of the requested work order.
 * @param userId - ID of the currently selected demo user.
 * @returns The maintenance request and its details.
 */
export async function getMaintenanceRequest(
    requestId: number,
    userId: number
): Promise<MaintenanceRequest> {

    return apiRequest<MaintenanceRequest>(
        `/requests/${requestId}?userId=${userId}`
    )
}


// --------------------------------------------------
// Create Maintenance Request
// --------------------------------------------------

/**
 * Submits a new maintenance request to the backend.
 *
 * The request object contains:
 *
 * - Title
 * - Description
 * - Building number
 * - Priority
 * - Building condition
 *
 * The backend automatically:
 *
 * - Associates the request with the submitting user.
 * - Associates the request with the user's department.
 * - Sets work-order status to OPEN.
 * - Sets approval status to PENDING_APPROVAL.
 * - Leaves the technician unassigned.
 *
 * A supervisor must approve the request before
 * the backend automatically assigns a technician.
 *
 * @param request - Form data for the new request.
 * @param requesterId - ID of the submitting user.
 * @returns The newly created maintenance request.
 */
export async function createMaintenanceRequest(
    request: CreateMaintenanceRequest,
    requesterId: number
): Promise<MaintenanceRequest> {

    return apiRequest<MaintenanceRequest>(
        `/requests?requesterId=${requesterId}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        }
    )
}


// --------------------------------------------------
// Work Order Comments and History Types
// --------------------------------------------------

/**
 * Represents a comment associated with a work order.
 *
 * Comments allow users to communicate about
 * maintenance issues without changing the
 * work-order lifecycle status.
 */
export interface WorkOrderComment {
    id: number

    author: {
        id: number
        name: string
        role: string
    }

    content: string
    createdAt: string
}

/**
 * Represents an entry in a work order's history.
 *
 * History entries provide an audit trail of:
 *
 * - Request submission.
 * - Supervisor approval.
 * - Automatic technician assignment.
 * - Work-order status changes.
 * - Technician notes.
 * - Completion.
 */
export interface WorkOrderHistory {
    id: number

    actor: {
        id: number
        name: string
    }

    status: RequestStatus
    note: string
    createdAt: string
}


// --------------------------------------------------
// Retrieve Work Order History
// --------------------------------------------------

/**
 * Retrieves the chronological history of a work order.
 *
 * The backend verifies that the current user
 * has permission to access the work order.
 *
 * @param requestId - ID of the work order.
 * @param userId - ID of the currently selected demo user.
 * @returns Chronological work-order history.
 */
export async function getRequestHistory(
    requestId: number,
    userId: number
): Promise<WorkOrderHistory[]> {

    return apiRequest<WorkOrderHistory[]>(
        `/requests/${requestId}/history?userId=${userId}`
    )
}


// --------------------------------------------------
// Retrieve Work Order Comments
// --------------------------------------------------

/**
 * Retrieves comments associated with a work order.
 *
 * The backend checks whether the current user
 * has permission to view the request.
 *
 * @param requestId - ID of the work order.
 * @param userId - ID of the currently selected demo user.
 * @returns Chronological list of comments.
 */
export async function getRequestComments(
    requestId: number,
    userId: number
): Promise<WorkOrderComment[]> {

    return apiRequest<WorkOrderComment[]>(
        `/requests/${requestId}/comments?userId=${userId}`
    )
}


// --------------------------------------------------
// Add Work Order Comment
// --------------------------------------------------

/**
 * Adds a comment to an existing work order.
 *
 * Requesters and supervisors can comment on
 * requests within their own department.
 *
 * Technicians can comment on requests
 * across departments.
 *
 * The backend validates access and rejects
 * empty comments.
 *
 * @param requestId - ID of the work order.
 * @param authorId - ID of the commenting user.
 * @param content - Comment text.
 * @returns The saved comment.
 */
export async function addRequestComment(
    requestId: number,
    authorId: number,
    content: string
): Promise<WorkOrderComment> {

    return apiRequest<WorkOrderComment>(
        `/requests/${requestId}/comments?authorId=${authorId}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain',
            },
            body: content,
        }
    )
}


// --------------------------------------------------
// Supervisor Approval and Automatic Assignment
// --------------------------------------------------

/**
 * Approves a maintenance request.
 *
 * Only supervisors can approve requests belonging
 * to their own department.
 *
 * The frontend does not select a technician.
 *
 * Instead, the backend:
 *
 * 1. Verifies the supervisor's role and department.
 * 2. Confirms the request is PENDING_APPROVAL.
 * 3. Retrieves all technicians.
 * 4. Counts each technician's active work orders.
 * 5. Selects the technician with the fewest assignments.
 * 6. Uses the lowest technician ID to break ties.
 * 7. Records the supervisor and approval timestamp.
 * 8. Assigns the selected technician.
 * 9. Records the approval and assignment in history.
 *
 * @param requestId - ID of the work order to approve.
 * @param supervisorId - ID of the approving supervisor.
 * @returns The approved request with its assigned technician.
 */
export async function approveRequest(
    requestId: number,
    supervisorId: number
): Promise<MaintenanceRequest> {

    return apiRequest<MaintenanceRequest>(
        `/requests/${requestId}/approve?supervisorId=${supervisorId}`,
        {
            method: 'PATCH',
        }
    )
}


// --------------------------------------------------
// Update Work Order Status
// --------------------------------------------------

/**
 * Updates the status of an approved work order.
 *
 * Only the assigned technician can transition
 * a request from OPEN to IN_PROGRESS.
 *
 * Completing a work order requires using
 * the separate completion endpoint.
 *
 * @param requestId - ID of the work order.
 * @param actorId - ID of the assigned technician.
 * @param status - Requested new status.
 * @returns The updated maintenance request.
 */
export async function updateRequestStatus(
    requestId: number,
    actorId: number,
    status: RequestStatus
): Promise<MaintenanceRequest> {

    return apiRequest<MaintenanceRequest>(
        `/requests/${requestId}/status?actorId=${actorId}`,
        {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(status),
        }
    )
}


// --------------------------------------------------
// Add Technician Note
// --------------------------------------------------

/**
 * Adds or updates technician notes.
 *
 * Only the assigned technician can modify
 * notes on an approved work order.
 *
 * Notes cannot be modified after completion.
 *
 * The backend records technician notes
 * in the work-order history.
 *
 * @param requestId - ID of the work order.
 * @param actorId - ID of the assigned technician.
 * @param note - Technician note.
 * @returns The updated maintenance request.
 */
export async function addTechnicianNote(
    requestId: number,
    actorId: number,
    note: string
): Promise<MaintenanceRequest> {

    return apiRequest<MaintenanceRequest>(
        `/requests/${requestId}/notes?actorId=${actorId}`,
        {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(note),
        }
    )
}


// --------------------------------------------------
// Update Estimated Completion
// --------------------------------------------------

/**
 * Updates the estimated completion date and time.
 *
 * Only the assigned technician can update
 * an approved work order.
 *
 * The backend requires a future date/time
 * and prevents updates after completion.
 *
 * @param requestId - ID of the work order.
 * @param actorId - ID of the assigned technician.
 * @param estimatedCompletionAt - Estimated completion time.
 * @returns The updated maintenance request.
 */
export async function updateEstimatedCompletion(
    requestId: number,
    actorId: number,
    estimatedCompletionAt: string
): Promise<MaintenanceRequest> {

    return apiRequest<MaintenanceRequest>(
        `/requests/${requestId}/estimated-completion?actorId=${actorId}`,
        {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(estimatedCompletionAt),
        }
    )
}


// --------------------------------------------------
// Complete Maintenance Request
// --------------------------------------------------

/**
 * Completes an approved maintenance request.
 *
 * Only the assigned technician can complete
 * a work order that is IN_PROGRESS.
 *
 * A resolution summary is required.
 *
 * When completed, the backend:
 *
 * - Changes status to COMPLETED.
 * - Saves the resolution summary.
 * - Records the completion timestamp.
 * - Adds an entry to work-order history.
 *
 * @param requestId - ID of the work order.
 * @param actorId - ID of the assigned technician.
 * @param resolutionSummary - Description of completed work.
 * @returns The completed maintenance request.
 */
export async function completeMaintenanceRequest(
    requestId: number,
    actorId: number,
    resolutionSummary: string
): Promise<MaintenanceRequest> {

    return apiRequest<MaintenanceRequest>(
        `/requests/${requestId}/complete?actorId=${actorId}`,
        {
            method: 'PATCH',
            headers: {
                'Content-Type': 'text/plain',
            },
            body: resolutionSummary,
        }
    )
}