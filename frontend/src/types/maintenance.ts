export type RequestStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED'

export type RequestPriority = 'EMERGENCY' | 'URGENT' | 'ROUTINE'

export type EquipmentCondition = 'GOOD' | 'FAIR' | 'POOR'

export interface Department {
    id: number
    name: string
}

export interface User {
    id: number
    name: string
    email: string
    role: 'REQUESTER' | 'TECHNICIAN' | 'SUPERVISOR'
    department: Department | null
}

export interface MaintenanceRequest {
    id: number
    title: string
    description: string
    buildingNumber: string
    priority: RequestPriority
    condition: EquipmentCondition
    status: RequestStatus
    requester: User
    department: Department
    assignedTechnician: User | null
    technicianNotes: string | null
    resolutionSummary: string | null
    createdAt: string
    updatedAt: string
    startedAt: string | null
    completedAt: string | null
    estimatedCompletionAt: string | null
}

export interface CreateMaintenanceRequest {
    title: string
    description: string
    buildingNumber: string
    priority: RequestPriority
    condition: EquipmentCondition
}