<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { currentUser, demoUsers } from '../stores/demoUser'

import {
  getMaintenanceRequest,
  getRequestHistory,
  getRequestComments,
  addRequestComment,
  assignTechnician,
  updateRequestStatus,
  addTechnicianNote,
  updateEstimatedCompletion,
  completeMaintenanceRequest,
} from '../services/maintenanceService'

import type {
  MaintenanceRequest,
  RequestPriority,
  RequestStatus,
} from '../types/maintenance'

import type {
  WorkOrderComment,
  WorkOrderHistory,
} from '../services/maintenanceService'

const route = useRoute()
const router = useRouter()

const requestId = Number(route.params.id)

const request = ref<MaintenanceRequest | null>(null)
const history = ref<WorkOrderHistory[]>([])
const comments = ref<WorkOrderComment[]>([])

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const selectedTechnicianId = ref<number | null>(null)
const technicianNote = ref('')
const estimatedCompletion = ref('')
const resolutionSummary = ref('')
const newComment = ref('')

const technicians = demoUsers.filter(
    user => user.role === 'TECHNICIAN'
)

const isSupervisor = computed(
    () => currentUser.value.role === 'SUPERVISOR'
)

const isAssignedTechnician = computed(
    () =>
        currentUser.value.role === 'TECHNICIAN' &&
        request.value?.assignedTechnician?.id === currentUser.value.id
)

const isCompleted = computed(
    () => request.value?.status === 'COMPLETED'
)

const canStart = computed(
    () => isAssignedTechnician.value &&
        request.value?.status === 'OPEN'
)

const canComplete = computed(
    () => isAssignedTechnician.value &&
        request.value?.status === 'IN_PROGRESS'
)

const canEdit = computed(
    () => isAssignedTechnician.value && !isCompleted.value
)

const canAssign = computed(
    () => isSupervisor.value && !isCompleted.value
)

const canView = computed(() => {
  if (!request.value) return false

  if (currentUser.value.role === 'TECHNICIAN') return true

  return request.value.department?.name === currentUser.value.department
})

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

function formatDate(date: string | null | undefined) {
  if (!date) return 'Not available'
  return new Date(date).toLocaleString()
}

async function loadRequest() {
  loading.value = true
  error.value = ''

  try {
    const loaded = await getMaintenanceRequest(requestId)

    request.value = loaded

    if (!canView.value) {
      request.value = null
      error.value = 'You do not have access to this work order.'
      return
    }

    selectedTechnicianId.value = loaded.assignedTechnician?.id ?? null

    const [loadedHistory, loadedComments] = await Promise.all([
      getRequestHistory(requestId),
      getRequestComments(requestId, currentUser.value.id),
    ])

    history.value = loadedHistory
    comments.value = loadedComments
  } catch (err) {
    error.value = err instanceof Error
        ? err.message
        : 'Failed to load work order.'
  } finally {
    loading.value = false
  }
}

async function runAction(action: () => Promise<unknown>, message: string) {
  saving.value = true
  error.value = ''
  success.value = ''

  try {
    await action()
    await loadRequest()
    success.value = message
  } catch (err) {
    error.value = err instanceof Error
        ? err.message
        : 'Operation failed.'
  } finally {
    saving.value = false
  }
}

async function handleAssignment() {
  if (selectedTechnicianId.value === null) return

  await runAction(
      () => assignTechnician(
          requestId,
          selectedTechnicianId.value!,
          currentUser.value.id
      ),
      'Technician assigned successfully.'
  )
}

async function handleStart() {
  await runAction(
      () => updateRequestStatus(
          requestId,
          currentUser.value.id,
          'IN_PROGRESS'
      ),
      'Work order started.'
  )
}

async function handleNote() {
  if (!technicianNote.value.trim()) return

  await runAction(
      () => addTechnicianNote(
          requestId,
          currentUser.value.id,
          technicianNote.value
      ),
      'Technician note saved.'
  )

  if (!error.value) technicianNote.value = ''
}

async function handleEstimatedCompletion() {
  if (!estimatedCompletion.value) return

  await runAction(
      () => updateEstimatedCompletion(
          requestId,
          currentUser.value.id,
          estimatedCompletion.value
      ),
      'Estimated completion updated.'
  )
}

async function handleCompletion() {
  if (!resolutionSummary.value.trim()) return

  await runAction(
      () => completeMaintenanceRequest(
          requestId,
          currentUser.value.id,
          resolutionSummary.value
      ),
      'Work order completed successfully.'
  )

  if (!error.value) resolutionSummary.value = ''
}

async function handleComment() {
  if (!newComment.value.trim()) return

  await runAction(
      () => addRequestComment(
          requestId,
          currentUser.value.id,
          newComment.value
      ),
      'Comment added successfully.'
  )

  if (!error.value) newComment.value = ''
}

onMounted(loadRequest)
</script>

<template>
  <div>
    <v-btn
        variant="text"
        prepend-icon="mdi-arrow-left"
        class="mb-4"
        @click="router.push('/requests')"
    >
      Back to Work Orders
    </v-btn>

    <v-progress-linear
        v-if="loading"
        indeterminate
        color="primary"
        class="mb-4"
    />

    <v-alert
        v-if="error"
        type="error"
        variant="tonal"
        class="mb-4"
    >
      {{ error }}
    </v-alert>

    <v-alert
        v-if="success"
        type="success"
        variant="tonal"
        class="mb-4"
    >
      {{ success }}
    </v-alert>

    <template v-if="request">
      <div class="d-flex align-center justify-space-between mb-6">
        <div>
          <h1 class="text-h4 font-weight-bold">
            Work Order #{{ request.id }}
          </h1>

          <p class="text-h6 mt-2">
            {{ request.title }}
          </p>
        </div>

        <div class="d-flex ga-2">
          <v-chip
              :color="priorityColor(request.priority)"
              variant="tonal"
          >
            {{ request.priority }}
          </v-chip>

          <v-chip
              :color="statusColor(request.status)"
              variant="tonal"
          >
            {{ formatStatus(request.status) }}
          </v-chip>
        </div>
      </div>

      <v-row>
        <v-col cols="12" md="8">
          <v-card class="mb-5" elevation="1">
            <v-card-title class="pa-5">
              Request Information
            </v-card-title>

            <v-divider />

            <v-card-text class="pa-5">
              <p class="mb-4">
                <strong>Description:</strong><br />
                {{ request.description }}
              </p>

              <v-row>
                <v-col cols="6">
                  <strong>Building:</strong>
                  {{ request.buildingNumber }}
                </v-col>

                <v-col cols="6">
                  <strong>Condition:</strong>
                  {{ request.condition }}
                </v-col>

                <v-col cols="6">
                  <strong>Department:</strong>
                  {{ request.department?.name }}
                </v-col>

                <v-col cols="6">
                  <strong>Requester:</strong>
                  {{ request.requester?.name }}
                </v-col>

                <v-col cols="6">
                  <strong>Assigned Technician:</strong>
                  {{ request.assignedTechnician?.name ?? 'Unassigned' }}
                </v-col>

                <v-col cols="6">
                  <strong>Created:</strong>
                  {{ formatDate(request.createdAt) }}
                </v-col>

                <v-col cols="6">
                  <strong>Started:</strong>
                  {{ formatDate(request.startedAt) }}
                </v-col>

                <v-col cols="6">
                  <strong>Estimated Completion:</strong>
                  {{ formatDate(request.estimatedCompletionAt) }}
                </v-col>

                <v-col cols="6">
                  <strong>Completed:</strong>
                  {{ formatDate(request.completedAt) }}
                </v-col>
              </v-row>

              <div v-if="request.technicianNotes" class="mt-5">
                <strong>Technician Notes:</strong>
                <p>{{ request.technicianNotes }}</p>
              </div>

              <div v-if="request.resolutionSummary" class="mt-5">
                <strong>Resolution Summary:</strong>
                <p>{{ request.resolutionSummary }}</p>
              </div>
            </v-card-text>
          </v-card>

          <v-card class="mb-5" elevation="1">
            <v-card-title class="pa-5">
              Comments
            </v-card-title>

            <v-divider />

            <v-card-text class="pa-5">
              <div
                  v-for="comment in comments"
                  :key="comment.id"
                  class="mb-4"
              >
                <div class="d-flex justify-space-between">
                  <strong>{{ comment.author.name }}</strong>
                  <span class="text-caption">
                    {{ formatDate(comment.createdAt) }}
                  </span>
                </div>

                <p class="mt-2">{{ comment.content }}</p>
                <v-divider class="mt-4" />
              </div>

              <p
                  v-if="comments.length === 0"
                  class="text-medium-emphasis mb-4"
              >
                No comments yet.
              </p>

              <v-textarea
                  v-model="newComment"
                  label="Add a Comment"
                  variant="outlined"
                  rows="3"
              />

              <v-btn
                  color="primary"
                  :loading="saving"
                  :disabled="!newComment.trim()"
                  @click="handleComment"
              >
                Post Comment
              </v-btn>
            </v-card-text>
          </v-card>

          <v-card elevation="1">
            <v-card-title class="pa-5">
              Work Order History
            </v-card-title>

            <v-divider />

            <v-card-text class="pa-5">
              <v-timeline
                  side="end"
                  density="compact"
              >
                <v-timeline-item
                    v-for="entry in history"
                    :key="entry.id"
                    dot-color="primary"
                    size="small"
                >
                  <strong>{{ formatStatus(entry.status) }}</strong>

                  <p>{{ entry.note }}</p>

                  <p class="text-caption text-medium-emphasis">
                    {{ entry.actor.name }} —
                    {{ formatDate(entry.createdAt) }}
                  </p>
                </v-timeline-item>
              </v-timeline>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="4">
          <v-card
              v-if="canAssign"
              class="mb-5"
              elevation="1"
          >
            <v-card-title class="pa-5">
              Supervisor Actions
            </v-card-title>

            <v-divider />

            <v-card-text class="pa-5">
              <v-select
                  v-model="selectedTechnicianId"
                  :items="technicians"
                  item-title="name"
                  item-value="id"
                  label="Assign Technician"
                  variant="outlined"
              />

              <v-btn
                  color="primary"
                  block
                  :loading="saving"
                  :disabled="selectedTechnicianId === null"
                  @click="handleAssignment"
              >
                Assign Technician
              </v-btn>
            </v-card-text>
          </v-card>

          <v-card
              v-if="isAssignedTechnician"
              elevation="1"
          >
            <v-card-title class="pa-5">
              Technician Actions
            </v-card-title>

            <v-divider />

            <v-card-text class="pa-5">
              <v-btn
                  v-if="canStart"
                  color="primary"
                  block
                  class="mb-5"
                  :loading="saving"
                  @click="handleStart"
              >
                Start Work Order
              </v-btn>

              <template v-if="canEdit">
                <v-textarea
                    v-model="technicianNote"
                    label="Technician Note"
                    variant="outlined"
                    rows="3"
                />

                <v-btn
                    color="primary"
                    block
                    class="mb-5"
                    :loading="saving"
                    :disabled="!technicianNote.trim()"
                    @click="handleNote"
                >
                  Save Note
                </v-btn>

                <v-text-field
                    v-model="estimatedCompletion"
                    type="datetime-local"
                    label="Estimated Completion"
                    variant="outlined"
                />

                <v-btn
                    color="primary"
                    block
                    class="mb-5"
                    :loading="saving"
                    :disabled="!estimatedCompletion"
                    @click="handleEstimatedCompletion"
                >
                  Update Estimate
                </v-btn>
              </template>

              <template v-if="canComplete">
                <v-divider class="mb-5" />

                <v-textarea
                    v-model="resolutionSummary"
                    label="Resolution Summary"
                    variant="outlined"
                    rows="4"
                />

                <v-btn
                    color="success"
                    block
                    :loading="saving"
                    :disabled="!resolutionSummary.trim()"
                    @click="handleCompletion"
                >
                  Complete Work Order
                </v-btn>
              </template>

              <v-alert
                  v-if="isCompleted"
                  type="success"
                  variant="tonal"
              >
                This work order is completed.
              </v-alert>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </template>
  </div>
</template>