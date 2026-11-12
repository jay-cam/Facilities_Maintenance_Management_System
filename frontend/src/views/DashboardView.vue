<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { currentUser } from '../stores/demoUser'
import { getMaintenanceRequests } from '../services/maintenanceService'

import type {
  MaintenanceRequest,
  RequestPriority,
  RequestStatus,
} from '../types/maintenance'


// --------------------------------------------------
// Router and Reactive State
// --------------------------------------------------

/**
 * Provides access to Vue Router.
 *
 * Used to navigate to:
 * - The Work Orders page
 * - Filtered work-order lists
 * - Individual work-order details
 * - The New Request form
 */
const router = useRouter()

/**
 * Stores maintenance requests retrieved from Spring Boot.
 *
 * Backend visibility rules:
 *
 * REQUESTER / SUPERVISOR:
 * Only requests belonging to their department.
 *
 * TECHNICIAN:
 * Requests across all departments.
 *
 * Frontend filtering further narrows these results.
 */
const requests = ref<MaintenanceRequest[]>([])

// Tracks API loading and error states.
const loading = ref(false)
const error = ref('')


// --------------------------------------------------
// Technician Dashboard Scope
// --------------------------------------------------

/**
 * Determines whether the selected demo user
 * has the TECHNICIAN role.
 */
const isTechnician = computed(
    () => currentUser.value.role === 'TECHNICIAN'
)

/**
 * Stores the technician's selected dashboard scope.
 *
 * Special values:
 *
 * MY_ASSIGNED:
 * Only work orders assigned to the current technician.
 *
 * ALL:
 * Work orders across every department.
 *
 * Any other value:
 * A specific department name.
 *
 * Technicians default to MY_ASSIGNED.
 *
 * Requesters and supervisors do not use this filter
 * because the backend already restricts their results.
 */
const selectedScope = ref('MY_ASSIGNED')

/**
 * Builds the technician's available dashboard filters.
 *
 * The first two options are always:
 *
 * - My Assigned Work Orders
 * - All Departments
 *
 * Department options are generated dynamically
 * from the work orders returned by the backend.
 *
 * Set removes duplicate department names.
 *
 * Departments without work orders are not included.
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
// Filtered Work Orders
// --------------------------------------------------

/**
 * Determines which work orders are included
 * in the dashboard.
 *
 * REQUESTER / SUPERVISOR:
 * Use the department-restricted results returned
 * by the backend.
 *
 * TECHNICIAN:
 *
 * MY_ASSIGNED:
 * Match assignedTechnician.id against currentUser.id.
 *
 * ALL:
 * Include every work order returned by the backend.
 *
 * Department name:
 * Include requests belonging to that department.
 *
 * All dashboard statistics use this computed property.
 *
 * This ensures that changing the dropdown updates
 * every statistic and the recent-work-orders list.
 */
const filteredRequests = computed(() => {

  // Requesters and supervisors use their department's requests.
  if (!isTechnician.value) {
    return requests.value
  }

  // Show only work orders assigned to the current technician.
  if (selectedScope.value === 'MY_ASSIGNED') {

    return requests.value.filter(
        request =>
            request.assignedTechnician?.id === currentUser.value.id
    )

  }

  // Show work orders across all departments.
  if (selectedScope.value === 'ALL') {
    return requests.value
  }

  // Show work orders belonging to a selected department.
  return requests.value.filter(
      request =>
          request.department?.name === selectedScope.value
  )

})

/**
 * Determines the descriptive label displayed
 * underneath the dashboard heading.
 *
 * Requesters and supervisors:
 * Their department name.
 *
 * Technicians:
 * Their currently selected scope.
 */
const dashboardScope = computed(() => {

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
// Dashboard Statistics
// --------------------------------------------------

/**
 * Counts all work orders within the current scope.
 */
const total = computed(
    () => filteredRequests.value.length
)

/**
 * Counts OPEN work orders within the current scope.
 */
const open = computed(
    () => filteredRequests.value.filter(
        request => request.status === 'OPEN'
    ).length
)

/**
 * Counts IN_PROGRESS work orders within the current scope.
 */
const inProgress = computed(
    () => filteredRequests.value.filter(
        request => request.status === 'IN_PROGRESS'
    ).length
)

/**
 * Counts COMPLETED work orders within the current scope.
 */
const completed = computed(
    () => filteredRequests.value.filter(
        request => request.status === 'COMPLETED'
    ).length
)

/**
 * Counts EMERGENCY work orders within the current scope.
 */
const emergency = computed(
    () => filteredRequests.value.filter(
        request => request.priority === 'EMERGENCY'
    ).length
)

/**
 * Calculates the completion percentage
 * for the currently displayed work orders.
 *
 * Formula:
 *
 * (Completed / Total) * 100
 *
 * Returns zero if there are no work orders.
 */
const completionRate = computed(() => {

  if (total.value === 0) {
    return 0
  }

  return Math.round(
      (completed.value / total.value) * 100
  )

})

/**
 * Retrieves the five newest work orders
 * within the currently selected scope.
 *
 * A copy of the array is sorted so the original
 * API response remains unchanged.
 */
const recentRequests = computed(() => {

  return [...filteredRequests.value]
      .sort(
          (a, b) =>
              new Date(b.createdAt).getTime() -
              new Date(a.createdAt).getTime()
      )
      .slice(0, 5)

})

/**
 * Calculates the number of work orders
 * belonging to each priority category.
 *
 * These counts always reflect the selected scope.
 */
const priorityCounts = computed(() => {

  return {
    EMERGENCY: filteredRequests.value.filter(
        request => request.priority === 'EMERGENCY'
    ).length,

    URGENT: filteredRequests.value.filter(
        request => request.priority === 'URGENT'
    ).length,

    ROUTINE: filteredRequests.value.filter(
        request => request.priority === 'ROUTINE'
    ).length,
  }

})


// --------------------------------------------------
// Dashboard Summary Cards
// --------------------------------------------------

/**
 * Defines the five dashboard summary cards.
 *
 * Each card contains:
 *
 * - A title
 * - A calculated value
 * - A Material Design icon
 * - A Vuetify color
 * - An optional status or priority filter
 *
 * The filter information is used when a user
 * clicks the card.
 *
 * Total Work Orders has no additional filter.
 */
const statistics = computed(() => [
  {
    title: 'Total Work Orders',
    value: total.value,
    icon: 'mdi-clipboard-text-outline',
    color: 'primary',
    status: undefined,
    priority: undefined,
  },
  {
    title: 'Open',
    value: open.value,
    icon: 'mdi-alert-circle-outline',
    color: 'secondary',
    status: 'OPEN',
    priority: undefined,
  },
  {
    title: 'In Progress',
    value: inProgress.value,
    icon: 'mdi-progress-clock',
    color: 'warning',
    status: 'IN_PROGRESS',
    priority: undefined,
  },
  {
    title: 'Completed',
    value: completed.value,
    icon: 'mdi-check-circle-outline',
    color: 'success',
    status: 'COMPLETED',
    priority: undefined,
  },
  {
    title: 'Emergency',
    value: emergency.value,
    icon: 'mdi-alert-outline',
    color: 'error',
    status: undefined,
    priority: 'EMERGENCY',
  },
])


// --------------------------------------------------
// Dashboard Card Navigation
// --------------------------------------------------

/**
 * Navigates to the Work Orders page when a
 * dashboard summary card is clicked.
 *
 * The destination receives URL query parameters.
 *
 * Examples:
 *
 * /requests?status=OPEN
 *
 * /requests?priority=EMERGENCY
 *
 * /requests?scope=MY_ASSIGNED&status=IN_PROGRESS
 *
 * /requests?scope=Port%20Operations&priority=EMERGENCY
 *
 * The scope parameter is included for technicians.
 *
 * This preserves the dashboard's selected scope
 * when navigating to the Work Orders page.
 *
 * Requesters and supervisors do not need a scope
 * parameter because their backend results are
 * already department-restricted.
 *
 * RequestsView.vue will read these parameters
 * and initialize its filters accordingly.
 */
function navigateToWorkOrders(
    status?: string,
    priority?: string
) {

  const query: Record<string, string> = {}

  // Preserve the technician's current dashboard scope.
  if (isTechnician.value) {
    query.scope = selectedScope.value
  }

  // Apply a status filter when provided.
  if (status) {
    query.status = status
  }

  // Apply a priority filter when provided.
  if (priority) {
    query.priority = priority
  }

  router.push({
    path: '/requests',
    query,
  })

}


// --------------------------------------------------
// Display Formatting Helpers
// --------------------------------------------------

/**
 * Returns the appropriate Vuetify color
 * for each work-order priority.
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
 * for each work-order status.
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
 * Converts backend status values into readable labels.
 *
 * Example:
 * IN_PROGRESS becomes IN PROGRESS.
 */
function formatStatus(status: string) {
  return status.replaceAll('_', ' ')
}


// --------------------------------------------------
// Load Dashboard Data
// --------------------------------------------------

/**
 * Retrieves maintenance requests from Spring Boot.
 *
 * maintenanceService.ts selects the appropriate endpoint:
 *
 * TECHNICIAN:
 * GET /api/requests?userId=...
 *
 * REQUESTER / SUPERVISOR:
 * GET /api/requests/department?userId=...
 *
 * The backend enforces role and department visibility.
 *
 * The frontend applies additional filtering
 * for technician dashboard preferences.
 */
async function loadDashboard() {

  loading.value = true
  error.value = ''

  // Clear previously loaded work orders.
  requests.value = []

  // Technicians default to their own assignments.
  selectedScope.value = 'MY_ASSIGNED'

  try {

    requests.value = await getMaintenanceRequests(
        currentUser.value.id,
        currentUser.value.role
    )

  } catch (err) {

    error.value = err instanceof Error
        ? err.message
        : 'Failed to load dashboard.'

  } finally {

    loading.value = false

  }

}


// --------------------------------------------------
// Component Initialization
// --------------------------------------------------

/**
 * Loads the dashboard when the component is mounted.
 *
 * App.vue uses currentUser.id as the router-view key.
 *
 * Switching demo users recreates the page component,
 * causing the appropriate data to be loaded.
 */
onMounted(loadDashboard)

</script>


<template>
  <div>

    <!-- --------------------------------------------------
         Dashboard Header
         -------------------------------------------------- -->

    <div class="d-flex align-center justify-space-between mb-6">

      <div>
        <h1 class="text-h4 font-weight-bold">
          Dashboard
        </h1>

        <!-- Display the current dashboard scope. -->
        <p class="text-medium-emphasis mt-1">
          Facilities maintenance overview —
          {{ dashboardScope }}
        </p>
      </div>

      <div class="d-flex align-center ga-3">

        <!--
          Technician dashboard scope selector.

          Defaults to My Assigned Work Orders.

          Technicians may switch to All Departments
          or select an individual department.
        -->
        <v-select
            v-if="isTechnician"
            v-model="selectedScope"
            :items="scopeOptions"
            item-title="title"
            item-value="value"
            label="Work Order Scope"
            variant="outlined"
            density="compact"
            hide-details
            style="width: 280px"
        />

        <!--
          Requesters and supervisors can submit
          maintenance requests.

          Technicians cannot create new requests.
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
    </div>


    <!-- --------------------------------------------------
         Loading and Error Messages
         -------------------------------------------------- -->

    <v-alert
        v-if="error"
        type="error"
        variant="tonal"
        class="mb-4"
    >
      {{ error }}
    </v-alert>

    <v-progress-linear
        v-if="loading"
        indeterminate
        color="primary"
        class="mb-5"
    />


    <!-- --------------------------------------------------
         Clickable Dashboard Summary Cards
         -------------------------------------------------- -->

    <!--
      All five cards navigate to the Work Orders page.

      Each card passes its corresponding filter
      and preserves the technician's selected scope.

      For example:

      My Assigned Work Orders + In Progress
      navigates to assigned, in-progress requests.

      Port Operations + Emergency
      navigates to emergency requests in Port Operations.
    -->
    <v-row class="mb-5">

      <v-col
          v-for="stat in statistics"
          :key="stat.title"
          cols="12"
          sm="6"
          md="4"
          lg="2"
      >

        <v-card
            elevation="1"
            class="pa-4"
            link
            :aria-label="`View ${stat.title} work orders`"
            @click="navigateToWorkOrders(stat.status, stat.priority)"
        >

          <v-icon
              :icon="stat.icon"
              :color="stat.color"
              size="32"
              class="mb-3"
          />

          <div class="text-h4 font-weight-bold">
            {{ stat.value }}
          </div>

          <div class="text-body-2 text-medium-emphasis">
            {{ stat.title }}
          </div>

        </v-card>

      </v-col>
    </v-row>


    <!-- --------------------------------------------------
         Dashboard Details
         -------------------------------------------------- -->

    <v-row>

      <!-- Recent work orders. -->
      <v-col cols="12" md="8">

        <v-card elevation="1">

          <v-card-title class="pa-5">
            Recent Work Orders
          </v-card-title>

          <v-divider />

          <v-list>

            <!--
              Display the five newest work orders
              within the currently selected scope.
            -->
            <v-list-item
                v-for="request in recentRequests"
                :key="request.id"
                @click="router.push(`/requests/${request.id}`)"
            >

              <template #prepend>
                <v-icon icon="mdi-clipboard-text-outline" />
              </template>

              <v-list-item-title>
                #{{ request.id }} — {{ request.title }}
              </v-list-item-title>

              <v-list-item-subtitle>
                Building {{ request.buildingNumber }}
                — {{ request.department?.name }}
              </v-list-item-subtitle>

              <template #append>
                <v-chip
                    :color="statusColor(request.status)"
                    size="small"
                    variant="tonal"
                >
                  {{ formatStatus(request.status) }}
                </v-chip>
              </template>

            </v-list-item>

            <!-- Display a message when no requests match the filter. -->
            <v-list-item v-if="recentRequests.length === 0">
              No maintenance requests found.
            </v-list-item>

          </v-list>
        </v-card>
      </v-col>


      <!-- Completion rate and priority breakdown. -->
      <v-col cols="12" md="4">

        <v-card elevation="1" class="pa-5">

          <div class="text-h6 mb-4">
            Completion Rate
          </div>

          <!--
            Circular progress indicator showing
            the percentage of completed work orders
            within the current dashboard scope.
          -->
          <div class="text-center">

            <v-progress-circular
                :model-value="completionRate"
                :size="160"
                :width="15"
                color="success"
            >
              <span class="text-h5 font-weight-bold">
                {{ completionRate }}%
              </span>
            </v-progress-circular>

          </div>

          <p class="text-body-2 text-medium-emphasis text-center mt-5">
            {{ completed }} of {{ total }} work orders completed
          </p>

          <v-divider class="my-5" />


          <!-- --------------------------------------------------
               Priority Overview
               -------------------------------------------------- -->

          <div class="text-subtitle-1 mb-3">
            Priority Overview
          </div>

          <!--
            Display the number of emergency, urgent,
            and routine work orders.

            All counts follow the current scope.
          -->
          <div
              v-for="priority in (['EMERGENCY', 'URGENT', 'ROUTINE'] as RequestPriority[])"
              :key="priority"
              class="d-flex align-center justify-space-between mb-3"
          >

            <v-chip
                :color="priorityColor(priority)"
                size="small"
                variant="tonal"
            >
              {{ priority }}
            </v-chip>

            <strong>
              {{ priorityCounts[priority] }}
            </strong>

          </div>

        </v-card>
      </v-col>

    </v-row>

  </div>
</template>