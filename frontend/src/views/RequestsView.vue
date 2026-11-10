<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { currentUser } from '../stores/demoUser'
import { getMaintenanceRequests } from '../services/maintenanceService'

import type {
  MaintenanceRequest,
  RequestStatus,
  RequestPriority,
} from '../types/maintenance'

const router = useRouter()

const requests = ref<MaintenanceRequest[]>([])
const loading = ref(false)
const error = ref('')

const search = ref('')
const statusFilter = ref<RequestStatus | 'ALL'>('ALL')
const priorityFilter = ref<RequestPriority | 'ALL'>('ALL')
const sortBy = ref('newest')

const statusOptions = [
  { title: 'All Statuses', value: 'ALL' },
  { title: 'Open', value: 'OPEN' },
  { title: 'In Progress', value: 'IN_PROGRESS' },
  { title: 'Completed', value: 'COMPLETED' },
]

const priorityOptions = [
  { title: 'All Priorities', value: 'ALL' },
  { title: 'Emergency', value: 'EMERGENCY' },
  { title: 'Urgent', value: 'URGENT' },
  { title: 'Routine', value: 'ROUTINE' },
]

const sortOptions = [
  { title: 'Newest First', value: 'newest' },
  { title: 'Oldest First', value: 'oldest' },
  { title: 'Priority: Highest First', value: 'priority' },
  { title: 'Status', value: 'status' },
]

const headers = [
  { title: 'ID', key: 'id' },
  { title: 'Work Order', key: 'title' },
  { title: 'Building', key: 'buildingNumber' },
  { title: 'Priority', key: 'priority' },
  { title: 'Status', key: 'status' },
  { title: 'Assigned Technician', key: 'assignedTechnician' },
  { title: 'Created', key: 'createdAt' },
  { title: 'Actions', key: 'actions', sortable: false },
]

const filteredRequests = computed(() => {
  let result = [...requests.value]

  if (search.value.trim()) {
    const query = search.value.toLowerCase().trim()

    result = result.filter(request =>
        request.title.toLowerCase().includes(query) ||
        request.description.toLowerCase().includes(query) ||
        request.buildingNumber.toLowerCase().includes(query) ||
        String(request.id).includes(query)
    )
  }

  if (statusFilter.value !== 'ALL') {
    result = result.filter(
        request => request.status === statusFilter.value
    )
  }

  if (priorityFilter.value !== 'ALL') {
    result = result.filter(
        request => request.priority === priorityFilter.value
    )
  }

  if (sortBy.value === 'newest') {
    result.sort((a, b) =>
        new Date(b.createdAt).getTime() -
        new Date(a.createdAt).getTime()
    )
  }

  if (sortBy.value === 'oldest') {
    result.sort((a, b) =>
        new Date(a.createdAt).getTime() -
        new Date(b.createdAt).getTime()
    )
  }

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

  if (sortBy.value === 'status') {
    result.sort((a, b) => a.status.localeCompare(b.status))
  }

  return result
})

async function loadRequests() {
  loading.value = true
  error.value = ''

  try {
    requests.value = await getMaintenanceRequests(
        currentUser.value.id,
        currentUser.value.role
    )
  } catch (err) {
    error.value = err instanceof Error
        ? err.message
        : 'Failed to load maintenance requests.'
  } finally {
    loading.value = false
  }
}

function priorityColor(priority: RequestPriority) {
  switch (priority) {
    case 'EMERGENCY': return 'error'
    case 'URGENT': return 'warning'
    case 'ROUTINE': return 'primary'
  }
}

function statusColor(status: RequestStatus) {
  switch (status) {
    case 'OPEN': return 'secondary'
    case 'IN_PROGRESS': return 'warning'
    case 'COMPLETED': return 'success'
  }
}

function formatStatus(status: string) {
  return status.replaceAll('_', ' ')
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString()
}

onMounted(loadRequests)
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">
          Work Orders
        </h1>
        <p class="text-medium-emphasis mt-1">
          View and manage facility maintenance requests.
        </p>
      </div>

      <v-btn
          v-if="currentUser.role !== 'TECHNICIAN'"
          color="primary"
          prepend-icon="mdi-plus"
          @click="router.push('/requests/new')"
      >
        New Request
      </v-btn>
    </div>

    <v-alert
        v-if="error"
        type="error"
        variant="tonal"
        class="mb-4"
    >
      {{ error }}
    </v-alert>

    <v-card elevation="1">
      <v-card-text>
        <v-row>
          <v-col cols="12" md="4">
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

          <v-col cols="12" md="4">
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

      <v-data-table
          :headers="headers"
          :items="filteredRequests"
          :loading="loading"
          item-value="id"
          hover
      >
        <template #item.priority="{ item }">
          <v-chip
              :color="priorityColor(item.priority)"
              size="small"
              variant="tonal"
          >
            {{ item.priority }}
          </v-chip>
        </template>

        <template #item.status="{ item }">
          <v-chip
              :color="statusColor(item.status)"
              size="small"
              variant="tonal"
          >
            {{ formatStatus(item.status) }}
          </v-chip>
        </template>

        <template #item.assignedTechnician="{ item }">
          {{ item.assignedTechnician?.name ?? 'Unassigned' }}
        </template>

        <template #item.createdAt="{ item }">
          {{ formatDate(item.createdAt) }}
        </template>

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