import { computed, ref } from 'vue'

// --------------------------------------------------
// User Roles and Data Structure
// --------------------------------------------------

/**
 * Defines the three roles supported by the application.
 *
 * REQUESTER:
 * Can submit maintenance requests and view work orders
 * belonging to their department.
 *
 * SUPERVISOR:
 * Can view department work orders, submit requests,
 * and assign technicians.
 *
 * TECHNICIAN:
 * Can view work orders across all departments but can
 * only modify work orders specifically assigned to them.
 */
export type UserRole = 'REQUESTER' | 'TECHNICIAN' | 'SUPERVISOR'

/**
 * Defines the information associated with each demo user.
 *
 * The ID corresponds to the user's primary key in PostgreSQL.
 * The role determines which application features are available.
 * The department identifies the user's organizational unit.
 */
export interface DemoUser {
    id: number
    name: string
    role: UserRole
    department: string
}


// --------------------------------------------------
// Demo User Data
// --------------------------------------------------

/**
 * Defines the nine users available in demonstration mode.
 *
 * These IDs match the records created by DemoDataInitializer
 * and verified in PostgreSQL.
 *
 * The application includes:
 * - 3 requesters
 * - 3 supervisors
 * - 3 technicians
 *
 * Requesters and supervisors represent three operational
 * departments. Technicians belong to the centralized
 * Service Technicians department.
 *
 * NOTE:
 * These users are hardcoded for demonstration purposes.
 * A production application would retrieve user information
 * from an authenticated backend session.
 */
export const demoUsers: DemoUser[] = [

    // --------------------------------------------------
    // Waterfront Operations
    // --------------------------------------------------

    {
        id: 1,
        name: 'John Smith',
        role: 'REQUESTER',
        department: 'Waterfront Operations',
    },

    {
        id: 3,
        name: 'Robert Jones',
        role: 'SUPERVISOR',
        department: 'Waterfront Operations',
    },


    // --------------------------------------------------
    // Port Operations
    // --------------------------------------------------

    {
        id: 4,
        name: 'Sarah Mitchell',
        role: 'REQUESTER',
        department: 'Port Operations',
    },

    {
        id: 5,
        name: 'Michael Torres',
        role: 'SUPERVISOR',
        department: 'Port Operations',
    },


    // --------------------------------------------------
    // Logistics & Supply
    // --------------------------------------------------

    {
        id: 6,
        name: 'Emily Carter',
        role: 'REQUESTER',
        department: 'Logistics & Supply',
    },

    {
        id: 7,
        name: 'David Chen',
        role: 'SUPERVISOR',
        department: 'Logistics & Supply',
    },


    // --------------------------------------------------
    // Service Technicians
    // --------------------------------------------------

    {
        id: 2,
        name: 'Jane Doe',
        role: 'TECHNICIAN',
        department: 'Service Technicians',
    },

    {
        id: 8,
        name: 'James Wilson',
        role: 'TECHNICIAN',
        department: 'Service Technicians',
    },

    {
        id: 9,
        name: 'Daniel Rodriguez',
        role: 'TECHNICIAN',
        department: 'Service Technicians',
    },
]


// --------------------------------------------------
// Selected Demo User
// --------------------------------------------------

/**
 * Stores the ID of the currently selected demo user.
 *
 * Vue's ref() makes this value reactive.
 *
 * When selectedUserId changes, any Vue component
 * depending on it can automatically update.
 *
 * The application starts with John Smith (ID 1).
 */
const selectedUserId = ref<number>(1)


/**
 * Returns the complete user object associated with
 * the currently selected user ID.
 *
 * Vue's computed() automatically recalculates this
 * value whenever selectedUserId changes.
 *
 * Example:
 *
 * selectedUserId = 4
 *
 * currentUser becomes:
 * Sarah Mitchell, REQUESTER, Port Operations.
 *
 * Components use currentUser to determine which
 * role-specific features and department data to display.
 */
export const currentUser = computed(
    () => demoUsers.find(user => user.id === selectedUserId.value)!
)


// --------------------------------------------------
// Switch Demo User
// --------------------------------------------------

/**
 * Changes the currently selected demo user.
 *
 * The function first verifies that the requested ID
 * exists in the demoUsers array.
 *
 * If the user exists, selectedUserId is updated.
 *
 * Because selectedUserId is reactive, Vue automatically
 * updates components that depend on currentUser.
 *
 * This allows us to demonstrate different roles
 * without implementing a full authentication system.
 *
 * @param userId - ID of the demo user to select.
 */
export function switchUser(userId: number) {

    // Prevent switching to a nonexistent demo user.
    if (demoUsers.some(user => user.id === userId)) {

        // Update the reactive user selection.
        selectedUserId.value = userId

    }
}