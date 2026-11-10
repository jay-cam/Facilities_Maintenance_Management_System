import { computed, ref } from 'vue'

export type UserRole = 'REQUESTER' | 'TECHNICIAN' | 'SUPERVISOR'

export interface DemoUser {
    id: number
    name: string
    role: UserRole
    department: string
}

export const demoUsers: DemoUser[] = [
    {
        id: 1,
        name: 'John Smith',
        role: 'REQUESTER',
        department: 'Waterfront Operations',
    },
    {
        id: 2,
        name: 'Jane Doe',
        role: 'TECHNICIAN',
        department: 'Ship Maintenance & Repair',
    },
    {
        id: 3,
        name: 'Robert Jones',
        role: 'SUPERVISOR',
        department: 'Waterfront Operations',
    },
]

const selectedUserId = ref<number>(1)

export const currentUser = computed(
    () => demoUsers.find(user => user.id === selectedUserId.value)!
)

export function switchUser(userId: number) {
    if (demoUsers.some(user => user.id === userId)) {
        selectedUserId.value = userId
    }
}