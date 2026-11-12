<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { currentUser } from '../stores/demoUser'
import { getMaintenanceRequests } from '../services/maintenanceService'

import type {
  MaintenanceRequest,
  RequestStatus,
  RequestPriority,
} from '../types/maintenance'


// --------------------------------------------------
// Router and Reactive State
// --------------------------------------------------

/**
 * Vue Router provides two related capabilities:
 *
 * useRouter():
 * Navigates to other application pages.
 *
 * useRoute():
 * Reads information about the current page,
 * including URL query parameters.
 *
 * Query parameters allow dashboard cards to
 * open the Work Orders page with filters applied.
 */
const router = useRouter()
const route = useRoute()

/**
 * Stores work orders retrieved from Spring Boot.
 *
 * Backend visibility:
 *
 * REQUESTER / SUPERVISOR:
 * Requests belonging to their department.
 *
 * TECHNICIAN:
 * Requests across all departments.
 *
 * The frontend applies additional filtering
 * to these results.
 */
const requests = ref<MaintenanceRequest[]>([])

// Tracks API loading and error states.
const loading = ref(false)
const error = ref('')


// --------------------------------------------------
// User Role
// --------------------------------------------------

/**
 * Determines whether the current demo user
 * has the TECHNICIAN role.
 *
 * Only technicians can select different
 * work-order scopes.
 */
const isTechnician = computed(
    () => currentUser.value.role === 'TECHNICIAN'
)


// --------------------------------------------------
// Search, Filters, and Sorting
// --------------------------------------------------

/**
 * Stores the user's search query.
 *
 * Searchable fields:
 * - Work-order title
 * - Description
 * - Building number
 * - Work-order ID
 */
const search = ref('')

/**
 * Stores the selected work-order status.
 *
 * ALL means no status restriction.
 */
const statusFilter = ref<RequestStatus | 'ALL'>('ALL')

/**
 * Stores the selected priority.
 *
 * ALL means no priority restriction.
 */
const priorityFilter = ref<RequestPriority | 'ALL'>('ALL')

/**
 * Stores the selected sorting method.
 *
 * Work orders default to newest first.
 */
const sortBy = ref('newest')

/**
 * Stores the technician's selected work-order scope.
 *
 * MY_ASSIGNED:
 * Only work orders assigned to the current technician.
 *
 * ALL:
 * Work orders across every department.
 *
 * Department name:
 * All work orders belonging to that department.
 *
 * The default is MY_ASSIGNED.
 *
 * Requesters and supervisors do not use this
 * selection because their backend results are
 * already restricted to their department.
 */
const selectedScope = ref('MY_ASSIGNED')


// --------------------------------------------------
// Filter Dropdown Options
// --------------------------------------------------

/**
 * Available work-order status filters.
 */
const statusOptions = [
  { title: 'All Statuses', value: 'ALL' },
  { title: 'Open', value: 'OPEN' },
  { title: 'In Progress', value: 'IN_PROGRESS' },
  { title: 'Completed', value: 'COMPLETED' },
]

/**
 * Available priority filters.
 */
const priorityOptions = [
  { title: 'All Priorities', value: 'ALL' },
  { title: 'Emergency', value: 'EMERGENCY' },
  { title: 'Urgent', value: 'URGENT' },
  { title: 'Routine', value: 'ROUTINE' },
]

/**
 * Available sorting methods.
 */
const sortOptions = [
  { title: 'Newest First', value: 'newest' },
  { title: 'Oldest First', value: 'oldest' },
  { title: 'Priority: Highest First', value: 'priority' },
  { title: 'Status', value: 'status' },
]

/**
 * Generates the technician's scope options.
 *
 * The first two choices are always:
 *
 * - My Assigned Work Orders
 * - All Departments
 *
 * Individual departments are generated from
 * the backend's returned work orders.
 *
 * Set removes duplicate department names.
 *
 * Departments without requests are not included.
 */
const scopeOptions = computed(() => {

  const departments = requests.value
      .map(request => request.department?.name)
      .filter((name): name is string => Boolean(name))

  const uniqueDepartments = [...new Set(departments)].sort()

  return [
    {
      title: 'My Assigned Work Orders',
      value: 'MY_ASSIGNED',
    },
    {
      title: 'All Departments',
      value: 'ALL',
    },
    ...uniqueDepartments.map(name => ({
      title: name,
      value: name,
    })),
  ]

})


// --------------------------------------------------
// Dashboard Navigation Parameters
// --------------------------------------------------

/**
 * Reads filtering information from the URL.
 *
 * DashboardView.vue sends query parameters
 * when someone clicks a summary card.
 *
 * Examples:
 *
 * /requests?scope=MY_ASSIGNED&status=OPEN
 *
 * /requests?scope=ALL&priority=EMERGENCY
 *
 * /requests?scope=Port%20Operations&status=IN_PROGRESS
 *
 * This function validates incoming values before
 * applying them to the filter controls.
 *
 * Invalid status and priority values are ignored.
 *
 * If a technician opens Work Orders directly,
 * the default scope is MY_ASSIGNED.
 */
function applyRouteFilters() {

  // --------------------------------------------------
  // Technician Scope
  // --------------------------------------------------

  /**
   * Only technicians can select a scope.
   *
   * Requesters and supervisors remain restricted
   * to their department by the backend.
   */
  if (isTechnician.value) {

    const scope = route.query.scope

    if (typeof scope === 'string') {

      // Accept special scopes and valid department names.
      const validScope = scopeOptions.value.some(
          option => option.value === scope
      )

      selectedScope.value = validScope
          ? scope
          : 'MY_ASSIGNED'

    } else {

      // Direct navigation defaults to assigned work orders.
      selectedScope.value = 'MY_ASSIGNED'

    }

  } else {

    selectedScope.value = 'MY_ASSIGNED'

  }


  // --------------------------------------------------
  // Status Filter
  // --------------------------------------------------

  const status = route.query.status

  if (
      status === 'OPEN' ||
      status === 'IN_PROGRESS' ||
      status === 'COMPLETED'
  ) {

    statusFilter.value = status

  } else {

    statusFilter.value = 'ALL'

  }


  // --------------------------------------------------
  // Priority Filter
  // --------------------------------------------------

  const priority = route.query.priority

  if (
      priority === 'EMERGENCY' ||
      priority === 'URGENT' ||
      priority === 'ROUTINE'
  ) {

    priorityFilter.value = priority

  } else {

    priorityFilter.value = 'ALL'

  }

}

/**
 * Reapplies URL filters if the query parameters
 * change while the Work Orders page is open.
 *
 * This supports navigation between filtered views
 * without requiring a full browser reload.
 */
watch(
    () => route.query,
    () => applyRouteFilters()
)


// --------------------------------------------------
// Work Order Table Configuration
// --------------------------------------------------

/**
 * Defines the columns displayed in the work-order table.
 *
 * Each key corresponds to a MaintenanceRequest field
 * or a custom Vuetify table slot.
 */
const headers = [
  { title: 'ID', key: 'id' },
  { title: 'Work Order', key: 'title' },
  { title: 'Department', key: 'department' },
  { title: 'Building', key: 'buildingNumber' },
  { title: 'Priority', key: 'priority' },
  { title: 'Status', key: 'status' },
  { title: 'Assigned Technician', key: 'assignedTechnician' },
  { title: 'Created', key: 'createdAt' },
  { title: 'Actions', key: 'actions', sortable: false },
]


// --------------------------------------------------
// Combined Filtering and Sorting
// --------------------------------------------------

/**
 * Determines which work orders appear in the table.
 *
 * Filtering order:
 *
 * 1. Technician scope
 * 2. Search query
 * 3. Status
 * 4. Priority
 * 5. Sorting
 *
 * Each filter operates on the previous result.
 *
 * Example:
 *
 * Jane Doe selects My Assigned Work Orders,
 * then filters by IN_PROGRESS.
 *
 * Only Jane's assigned, in-progress work orders
 * will appear.
 *
 * Alternatively, selecting Port Operations
 * shows all requests in that department,
 * regardless of which technician is assigned.
 *
 * Vue automatically recalculates this computed
 * property whenever a filter changes.
 */
const filteredRequests = computed(() => {

  // Copy the array to preserve the original API response.
  let result = [...requests.value]


  // --------------------------------------------------
  // Technician Scope Filter
  // --------------------------------------------------

  if (isTechnician.value) {

    // Show only work orders assigned to this technician.
    if (selectedScope.value === 'MY_ASSIGNED') {

      result = result.filter(
          request =>
              request.assignedTechnician?.id === currentUser.value.id
      )

    } else if (selectedScope.value !== 'ALL') {

      // Show every work order in the selected department.
      result = result.filter(
          request =>
              request.department?.name === selectedScope.value
      )

    }

    // ALL leaves the full backend result unchanged.

  }


  // --------------------------------------------------
  // Search Filter
  // --------------------------------------------------

  /**
   * Searches multiple work-order fields.
   *
   * Search is case-insensitive.
   */
  if (search.value.trim()) {

    const query = search.value.toLowerCase().trim()

    result = result.filter(request =>
        request.title.toLowerCase().includes(query) ||
        request.description.toLowerCase().includes(query) ||
        request.buildingNumber.toLowerCase().includes(query) ||
        String(request.id).includes(query)
    )

  }


  // --------------------------------------------------
  // Status Filter
  // --------------------------------------------------

  if (statusFilter.value !== 'ALL') {

    result = result.filter(
        request => request.status === statusFilter.value
    )

  }


  // --------------------------------------------------
  // Priority Filter
  // --------------------------------------------------

  if (priorityFilter.value !== 'ALL') {

    result = result.filter(
        request => request.priority === priorityFilter.value
    )

  }


  // --------------------------------------------------
  // Sorting
  // --------------------------------------------------

  // Newest work orders first.
  if (sortBy.value === 'newest') {

    result.sort((a, b) =>
        new Date(b.createdAt).getTime() -
        new Date(a.createdAt).getTime()
    )

  }

  // Oldest work orders first.
  if (sortBy.value === 'oldest') {

    result.sort((a, b) =>
        new Date(a.createdAt).getTime() -
        new Date(b.createdAt).getTime()
    )

  }

  // Highest priority first.
  if (sortBy.value === 'priority') {

    const priorityOrder: Record<RequestPriority, number> = {
      EMERGENCY: 0,
      URGENT: 1,
      ROUTINE: 2,
    }

    result.sort((a, b) =>
        priorityOrder[a.priority] - priorityOrder[b.priority]
    )

  }

  // Sort alphabetically by backend status.
  if (sortBy.value === 'status') {

    result.sort(
        (a, b) => a.status.localeCompare(b.status)
    )

  }

  return result

})


// --------------------------------------------------
// Work Order Scope Label
// --------------------------------------------------

/**
 * Displays the current scope underneath the page title.
 *
 * Requesters and supervisors see their department.
 *
 * Technicians see:
 * - My Assigned Work Orders
 * - All Departments
 * - The selected department name
 */
const workOrderScope = computed(() => {

  if (!isTechnician.value) {
    return currentUser.value.department
  }

  if (selectedScope.value === 'MY_ASSIGNED') {
    return 'My Assigned Work Orders'
  }

  if (selectedScope.value === 'ALL') {
    return 'All Departments'
  }

  return selectedScope.value

})


// --------------------------------------------------
// Retrieve Work Orders
// --------------------------------------------------

/**
 * Retrieves work orders from Spring Boot.
 *
 * maintenanceService.ts selects the endpoint:
 *
 * TECHNICIAN:
 * GET /api/requests?userId=...
 *
 * REQUESTER / SUPERVISOR:
 * GET /api/requests/department?userId=...
 *
 * The backend determines which records are visible.
 *
 * Once the requests are loaded, the URL filters
 * are applied so department selections can be
 * validated against the available work orders.
 */
async function loadRequests() {

  loading.value = true
  error.value = ''

  // Clear previously loaded data.
  requests.value = []

  try {

    requests.value = await getMaintenanceRequests(
        currentUser.value.id,
        currentUser.value.role
    )

    // Apply dashboard filters after loading department options.
    applyRouteFilters()

  } catch (err) {

    error.value = err instanceof Error
        ? err.message
        : 'Failed to load maintenance requests.'

  } finally {

    loading.value = false

  }

}


// --------------------------------------------------
// Display Formatting Helpers
// --------------------------------------------------

/**
 * Returns the appropriate Vuetify color
 * for each priority.
 */
function priorityColor(priority: RequestPriority) {

  switch (priority) {
    case 'EMERGENCY':
      return 'error'

    case 'URGENT':
      return 'warning'

    case 'ROUTINE':
      return 'primary'
  }
}

/**
 * Returns the appropriate Vuetify color
 * for each status.
 */
function statusColor(status: RequestStatus) {

  switch (status) {
    case 'OPEN':
      return 'secondary'

    case 'IN_PROGRESS':
      return 'warning'

    case 'COMPLETED':
      return 'success'
  }
}

/**
 * Converts backend status values to readable labels.
 *
 * Example:
 * IN_PROGRESS becomes IN PROGRESS.
 */
function formatStatus(status: string) {
  return status.replaceAll('_', ' ')
}

/**
 * Formats an ISO timestamp as a local date.
 */
function formatDate(date: string) {
  return new Date(date).toLocaleDateString()
}


// --------------------------------------------------
// Component Initialization
// --------------------------------------------------

/**
 * Loads work orders when the component is mounted.
 *
 * App.vue recreates the page whenever the selected
 * demo user changes.
 *
 * This ensures work orders are fetched using
 * the currently selected user's role and ID.
 */
onMounted(loadRequests)

</script>


<template>
  <div>

    <!-- --------------------------------------------------
         Page Header
         -------------------------------------------------- -->

    <div class="d-flex align-center justify-space-between mb-6">

      <div>

        <h1 class="text-h4 font-weight-bold">
          Work Orders
        </h1>

        <!-- Display the currently selected work-order scope. -->
        <p class="text-medium-emphasis mt-1">
          View and manage facility maintenance requests —
          {{ workOrderScope }}
        </p>

      </div>

      <!--
        Requesters and supervisors can create
        new maintenance requests.
      -->
      <v-btn
          v-if="!isTechnician"
          color="primary"
          prepend-icon="mdi-plus"
          @click="router.push('/requests/new')"
      >
        New Request
      </v-btn>

    </div>


    <!-- --------------------------------------------------
         Error Messages
         -------------------------------------------------- -->

    <v-alert
        v-if="error"
        type="error"
        variant="tonal"
        class="mb-4"
    >
      {{ error }}
    </v-alert>


    <!-- --------------------------------------------------
         Search and Filter Controls
         -------------------------------------------------- -->

    <v-card elevation="1">

      <v-card-text>

        <v-row>

          <!-- Search by title, description, building, or ID. -->
          <v-col
              cols="12"
              :md="isTechnician ? 3 : 4"
          >
            <v-text-field
                v-model="search"
                label="Search work orders"
                prepend-inner-icon="mdi-magnify"
                variant="outlined"
                density="comfortable"
                hide-details
                clearable
            />
          </v-col>


          <!--
            Technician scope selector.

            Defaults to My Assigned Work Orders.

            Other options:
            - All Departments
            - Individual departments
          -->
          <v-col
              v-if="isTechnician"
              cols="12"
              md="3"
          >
            <v-select
                v-model="selectedScope"
                :items="scopeOptions"
                item-title="title"
                item-value="value"
                label="Work Order Scope"
                variant="outlined"
                density="comfortable"
                hide-details
            />
          </v-col>


          <!-- Status filter. -->
          <v-col cols="12" md="2">
            <v-select
                v-model="statusFilter"
                :items="statusOptions"
                label="Status"
                variant="outlined"
                density="comfortable"
                hide-details
            />
          </v-col>


          <!-- Priority filter. -->
          <v-col cols="12" md="2">
            <v-select
                v-model="priorityFilter"
                :items="priorityOptions"
                label="Priority"
                variant="outlined"
                density="comfortable"
                hide-details
            />
          </v-col>


          <!-- Sorting options. -->
          <v-col
              cols="12"
              :md="isTechnician ? 2 : 4"
          >
            <v-select
                v-model="sortBy"
                :items="sortOptions"
                label="Sort By"
                variant="outlined"
                density="comfortable"
                hide-details
            />
          </v-col>

        </v-row>

      </v-card-text>


      <!-- --------------------------------------------------
           Work Order Table
           -------------------------------------------------- -->

      <!--
        Vuetify renders the filtered work orders.

        Pagination and table rendering are handled
        by v-data-table.

        Changing any filter automatically updates
        filteredRequests and therefore the table.
      -->
      <v-data-table
          :headers="headers"
          :items="filteredRequests"
          :loading="loading"
          item-value="id"
          hover
      >

        <!-- Display the work order's department. -->
        <template #item.department="{ item }">
          {{ item.department?.name ?? 'Not assigned' }}
        </template>


        <!-- Display a colored priority chip. -->
        <template #item.priority="{ item }">
          <v-chip
              :color="priorityColor(item.priority)"
              size="small"
              variant="tonal"
          >
            {{ item.priority }}
          </v-chip>
        </template>


        <!-- Display a colored status chip. -->
        <template #item.status="{ item }">
          <v-chip
              :color="statusColor(item.status)"
              size="small"
              variant="tonal"
          >
            {{ formatStatus(item.status) }}
          </v-chip>
        </template>


        <!-- Display the assigned technician's name. -->
        <template #item.assignedTechnician="{ item }">
          {{ item.assignedTechnician?.name ?? 'Unassigned' }}
        </template>


        <!-- Format the creation date. -->
        <template #item.createdAt="{ item }">
          {{ formatDate(item.createdAt) }}
        </template>


        <!-- Open the selected work-order details. -->
        <template #item.actions="{ item }">
          <v-btn
              size="small"
              variant="text"
              color="primary"
              @click="router.push(`/requests/${item.id}`)"
          >
            View
          </v-btn>
        </template>

      </v-data-table>

    </v-card>

  </div>
</template>