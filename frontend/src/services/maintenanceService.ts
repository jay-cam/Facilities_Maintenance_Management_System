import { apiRequest } from './api'
import type {
    MaintenanceRequest,
    CreateMaintenanceRequest,
    RequestStatus,
} from '../types/maintenance'

export async function getMaintenanceRequests(
    userId: number,
    role: string
): Promise<MaintenanceRequest[]> {

    if (role === 'TECHNICIAN') {
        return apiRequest<MaintenanceRequest[]>('/requests')
    }

    return apiRequest<MaintenanceRequest[]>(
        `/requests/department?userId=${userId}`
    )
}

export async function getMaintenanceRequest(
    requestId: number
): Promise<MaintenanceRequest> {

    return apiRequest<MaintenanceRequest>(
        `/requests/${requestId}`
    )
}

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

export async function getRequestHistory(
    requestId: number
): Promise<WorkOrderHistory[]> {
    return apiRequest<WorkOrderHistory[]>(
        `/requests/${requestId}/history`
    )
}

export async function getRequestComments(
    requestId: number,
    userId: number
): Promise<WorkOrderComment[]> {
    return apiRequest<WorkOrderComment[]>(
        `/requests/${requestId}/comments?userId=${userId}`
    )
}

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

export async function assignTechnician(
    requestId: number,
    technicianId: number,
    supervisorId: number
): Promise<MaintenanceRequest> {
    return apiRequest<MaintenanceRequest>(
        `/requests/${requestId}/assign?technicianId=${technicianId}&supervisorId=${supervisorId}`,
        {
            method: 'PATCH',
        }
    )
}

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