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

const router = useRouter()

const requests = ref<MaintenanceRequest[]>([])
const loading = ref(false)
const error = ref('')

const total = computed(() => requests.value.length)

const open = computed(
    () => requests.value.filter(r => r.status === 'OPEN').length
)

const inProgress = computed(
    () => requests.value.filter(r => r.status === 'IN_PROGRESS').length
)

const completed = computed(
    () => requests.value.filter(r => r.status === 'COMPLETED').length
)

const emergency = computed(
    () => requests.value.filter(r => r.priority === 'EMERGENCY').length
)

const completionRate = computed(() =>
    total.value === 0
        ? 0
        : Math.round((completed.value / total.value) * 100)
)

const recentRequests = computed(() =>
    [...requests.value]
        .sort((a, b) =>
            new Date(b.createdAt).getTime() -
            new Date(a.createdAt).getTime()
        )
        .slice(0, 5)
)

const statistics = computed(() => [
  {
    title: 'Total Work Orders',
    value: total.value,
    icon: 'mdi-clipboard-text-outline',
    color: 'primary',
  },
  {
    title: 'Open',
    value: open.value,
    icon: 'mdi-alert-circle-outline',
    color: 'secondary',
  },
  {
    title: 'In Progress',
    value: inProgress.value,
    icon: 'mdi-progress-clock',
    color: 'warning',
  },
  {
    title: 'Completed',
    value: completed.value,
    icon: 'mdi-check-circle-outline',
    color: 'success',
  },
  {
    title: 'Emergency',
    value: emergency.value,
    icon: 'mdi-alert-outline',
    color: 'error',
  },
])

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

async function loadDashboard() {
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
        : 'Failed to load dashboard.'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">
          Dashboard
        </h1>

        <p class="text-medium-emphasis mt-1">
          Facilities maintenance overview —
          {{ currentUser.department }}
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

    <v-progress-linear
        v-if="loading"
        indeterminate
        color="primary"
        class="mb-5"
    />

    <v-row class="mb-5">
      <v-col
          v-for="stat in statistics"
          :key="stat.title"
          cols="12"
          sm="6"
          md="4"
          lg="2"
      >
        <v-card elevation="1" class="pa-4">
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

    <v-row>
      <v-col cols="12" md="8">
        <v-card elevation="1">
          <v-card-title class="pa-5">
            Recent Work Orders
          </v-card-title>

          <v-divider />

          <v-list>
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

            <v-list-item v-if="recentRequests.length === 0">
              No maintenance requests found.
            </v-list-item>
          </v-list>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card elevation="1" class="pa-5">
          <div class="text-h6 mb-4">
            Completion Rate
          </div>

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

          <div class="text-subtitle-1 mb-3">
            Priority Overview
          </div>

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
              {{ requests.filter(r => r.priority === priority).length }}
            </strong>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>