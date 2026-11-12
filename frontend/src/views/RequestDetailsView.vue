<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { currentUser } from '../stores/demoUser'

import {
  getMaintenanceRequest,
  getRequestHistory,
  getRequestComments,
  addRequestComment,
  approveRequest,
  updateRequestStatus,
  addTechnicianNote,
  updateEstimatedCompletion,
  completeMaintenanceRequest,
} from '../services/maintenanceService'

import type {
  ApprovalStatus,
  MaintenanceRequest,
  RequestPriority,
  RequestStatus,
} from '../types/maintenance'

import type {
  WorkOrderComment,
  WorkOrderHistory,
} from '../services/maintenanceService'


// --------------------------------------------------
// Router and Work Order Identification
// --------------------------------------------------

// Access the current route and Vue Router navigation functions.
const route = useRoute()
const router = useRouter()

// Extract the work-order ID from the URL.
// Example: /requests/8 identifies work order #8.
const requestId = Number(route.params.id)


// --------------------------------------------------
// Reactive Component State
// --------------------------------------------------

// Store the work order and its associated history and comments.
const request = ref<MaintenanceRequest | null>(null)
const history = ref<WorkOrderHistory[]>([])
const comments = ref<WorkOrderComment[]>([])

// Track loading, saving, and user-facing notifications.
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

// Store user input for technician actions and comments.
const technicianNote = ref('')
const estimatedCompletion = ref('')
const resolutionSummary = ref('')
const newComment = ref('')


// --------------------------------------------------
// Role-Based Permissions
// --------------------------------------------------

/**
 * Determines whether the current user is a supervisor.
 *
 * Supervisors can approve maintenance requests
 * belonging to their own department.
 */
const isSupervisor = computed(
    () => currentUser.value.role === 'SUPERVISOR'
)

/**
 * Determines whether the current work order
 * has received supervisor approval.
 *
 * Approval is tracked independently of
 * the work-order lifecycle status.
 */
const isApproved = computed(
    () => request.value?.approvalStatus === 'APPROVED'
)

/**
 * Determines whether the current user is the technician
 * assigned to this specific work order.
 *
 * Simply having the TECHNICIAN role does not grant
 * permission to modify every work order.
 */
const isAssignedTechnician = computed(
    () =>
        currentUser.value.role === 'TECHNICIAN' &&
        request.value?.assignedTechnician?.id === currentUser.value.id
)

/**
 * Checks whether the work order has been completed.
 *
 * Completed work orders cannot be modified
 * through the existing technician action endpoints.
 */
const isCompleted = computed(
    () => request.value?.status === 'COMPLETED'
)

/**
 * Supervisors can approve work orders only when:
 *
 * - They have the SUPERVISOR role.
 * - The request belongs to their department.
 * - The request is awaiting approval.
 * - The work order has not been completed.
 *
 * The backend independently verifies these rules.
 */
const canApprove = computed(
    () =>
        isSupervisor.value &&
        request.value?.department?.name === currentUser.value.department &&
        request.value?.approvalStatus === 'PENDING_APPROVAL' &&
        !isCompleted.value
)

/**
 * The assigned technician can start a work order
 * only when it has been approved and is OPEN.
 */
const canStart = computed(
    () =>
        isAssignedTechnician.value &&
        isApproved.value &&
        request.value?.status === 'OPEN'
)

/**
 * The assigned technician can complete a work order
 * only when it has been approved and is IN_PROGRESS.
 */
const canComplete = computed(
    () =>
        isAssignedTechnician.value &&
        isApproved.value &&
        request.value?.status === 'IN_PROGRESS'
)

/**
 * The assigned technician can edit notes and estimates
 * only after approval and before completion.
 */
const canEdit = computed(
    () =>
        isAssignedTechnician.value &&
        isApproved.value &&
        !isCompleted.value
)

/**
 * Determines whether the current user should be able
 * to view the selected work order.
 *
 * Technicians can view all departments.
 *
 * Requesters and supervisors can only view work orders
 * belonging to their department.
 *
 * IMPORTANT:
 * This frontend check controls the UI.
 * The Spring Boot service independently checks access.
 */
const canView = computed(() => {

  if (!request.value) {
    return false
  }

  if (currentUser.value.role === 'TECHNICIAN') {
    return true
  }

  return request.value.department?.name === currentUser.value.department
})


// --------------------------------------------------
// Display Formatting Helpers
// --------------------------------------------------

/**
 * Returns the Vuetify color associated with a priority.
 *
 * Emergency requests are red, urgent requests are
 * orange, and routine requests use the primary color.
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
 * Returns the Vuetify color associated with a status.
 *
 * These colors make work-order progress easier
 * to identify throughout the interface.
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
 * Returns the Vuetify color associated with
 * a supervisor approval state.
 *
 * Pending requests are highlighted in warning colors.
 * Approved requests are highlighted in success colors.
 */
function approvalColor(status: ApprovalStatus) {

  switch (status) {
    case 'PENDING_APPROVAL':
      return 'warning'

    case 'APPROVED':
      return 'success'
  }
}

/**
 * Converts backend enum values into readable labels.
 *
 * Example:
 * IN_PROGRESS becomes IN PROGRESS.
 */
function formatStatus(status: string) {
  return status.replaceAll('_', ' ')
}

/**
 * Converts an ISO date/time string into a
 * human-readable date using the browser's locale.
 *
 * Missing dates display "Not available".
 */
function formatDate(date: string | null | undefined) {

  if (!date) {
    return 'Not available'
  }

  return new Date(date).toLocaleString()
}


// --------------------------------------------------
// Load Work Order Details
// --------------------------------------------------

/**
 * Loads the selected work order from the backend.
 *
 * The process:
 *
 * 1. Retrieve the work order using the current user's ID.
 * 2. Verify the user can view the work order.
 * 3. Retrieve the work-order history and comments.
 * 4. Update the interface with the retrieved information.
 *
 * The backend enforces department restrictions for
 * individual requests, history, and comments.
 */
async function loadRequest() {

  loading.value = true
  error.value = ''

  // Clear previously loaded data before requesting new data.
  request.value = null
  history.value = []
  comments.value = []

  try {

    // Pass the current demo user's ID for backend access checks.
    const loaded = await getMaintenanceRequest(
        requestId,
        currentUser.value.id
    )

    request.value = loaded

    // Additional frontend safeguard against department mismatches.
    if (!canView.value) {
      request.value = null
      error.value = 'You do not have access to this work order.'
      return
    }

    // Retrieve history and comments concurrently.
    const [loadedHistory, loadedComments] = await Promise.all([

      getRequestHistory(
          requestId,
          currentUser.value.id
      ),

      getRequestComments(
          requestId,
          currentUser.value.id
      ),

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


// --------------------------------------------------
// Shared Action Handler
// --------------------------------------------------

/**
 * Executes a backend operation and refreshes the page.
 *
 * This helper prevents repeating the same loading,
 * error handling, and refresh logic for every action.
 *
 * @param action - Async function performing the API request.
 * @param message - Success message displayed after completion.
 */
async function runAction(
    action: () => Promise<unknown>,
    message: string
) {

  saving.value = true
  error.value = ''
  success.value = ''

  try {

    // Execute the requested backend operation.
    await action()

    // Reload the work order to display the latest changes.
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


// --------------------------------------------------
// Supervisor Actions
// --------------------------------------------------

/**
 * Approves a maintenance request.
 *
 * The backend verifies:
 * - The acting user is a supervisor.
 * - The request belongs to the supervisor's department.
 * - The request is awaiting approval.
 *
 * Once approved, the backend automatically assigns
 * the technician with the fewest active work orders.
 *
 * The approving supervisor, timestamp, and technician
 * assignment are saved to PostgreSQL.
 */
async function handleApproval() {

  if (!canApprove.value) {
    return
  }

  await runAction(
      () => approveRequest(
          requestId,
          currentUser.value.id
      ),
      'Work order approved and technician assigned automatically.'
  )
}


// --------------------------------------------------
// Technician Actions
// --------------------------------------------------

/**
 * Starts the assigned work order.
 *
 * The backend changes its status from OPEN to
 * IN_PROGRESS and records the start timestamp.
 */
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

/**
 * Saves a technician note.
 *
 * Only the assigned technician can add notes.
 * The backend also records the note in work-order history.
 */
async function handleNote() {

  if (!technicianNote.value.trim()) {
    return
  }

  await runAction(
      () => addTechnicianNote(
          requestId,
          currentUser.value.id,
          technicianNote.value
      ),
      'Technician note saved.'
  )

  // Clear the input only when the operation succeeds.
  if (!error.value) {
    technicianNote.value = ''
  }
}

/**
 * Updates the estimated completion date and time.
 *
 * The backend verifies that the estimate is in the future
 * and that the technician is assigned to the work order.
 */
async function handleEstimatedCompletion() {

  if (!estimatedCompletion.value) {
    return
  }

  await runAction(
      () => updateEstimatedCompletion(
          requestId,
          currentUser.value.id,
          estimatedCompletion.value
      ),
      'Estimated completion updated.'
  )
}

/**
 * Completes the assigned work order.
 *
 * The backend requires:
 * - The work order to be IN_PROGRESS.
 * - A resolution summary.
 * - The acting user to be the assigned technician.
 *
 * Completion also creates a history entry.
 */
async function handleCompletion() {

  if (!resolutionSummary.value.trim()) {
    return
  }

  await runAction(
      () => completeMaintenanceRequest(
          requestId,
          currentUser.value.id,
          resolutionSummary.value
      ),
      'Work order completed successfully.'
  )

  if (!error.value) {
    resolutionSummary.value = ''
  }
}


// --------------------------------------------------
// Comments
// --------------------------------------------------

/**
 * Adds a comment to the current work order.
 *
 * Requesters and supervisors can comment within
 * their department.
 *
 * Technicians can comment across departments.
 *
 * The backend validates access before saving.
 */
async function handleComment() {

  if (!newComment.value.trim()) {
    return
  }

  await runAction(
      () => addRequestComment(
          requestId,
          currentUser.value.id,
          newComment.value
      ),
      'Comment added successfully.'
  )

  if (!error.value) {
    newComment.value = ''
  }
}


// --------------------------------------------------
// Component Initialization
// --------------------------------------------------

// Load the work order when this component is mounted.
onMounted(loadRequest)

</script>

<template>
  <div>
    <!-- Return to the work-order list. -->
    <v-btn
        variant="text"
        prepend-icon="mdi-arrow-left"
        class="mb-4"
        @click="router.push('/requests')"
    >
      Back to Work Orders
    </v-btn>

    <!-- Display a loading indicator while retrieving data. -->
    <v-progress-linear
        v-if="loading"
        indeterminate
        color="primary"
        class="mb-4"
    />

    <!-- Display errors returned by the frontend or backend. -->
    <v-alert
        v-if="error"
        type="error"
        variant="tonal"
        class="mb-4"
    >
      {{ error }}
    </v-alert>

    <!-- Display confirmation messages after successful actions. -->
    <v-alert
        v-if="success"
        type="success"
        variant="tonal"
        class="mb-4"
    >
      {{ success }}
    </v-alert>

    <template v-if="request">

      <!-- Work-order title, priority, status, and approval state. -->
      <div class="d-flex align-center justify-space-between mb-6">
        <div>
          <h1 class="text-h4 font-weight-bold">
            Work Order #{{ request.id }}
          </h1>

          <p class="text-h6 mt-2">
            {{ request.title }}
          </p>
        </div>

        <div class="d-flex ga-2 flex-wrap justify-end">
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

          <!-- Approval state is displayed independently of work status. -->
          <v-chip
              :color="approvalColor(request.approvalStatus)"
              variant="tonal"
          >
            {{ formatStatus(request.approvalStatus) }}
          </v-chip>
        </div>
      </div>

      <v-row>
        <!-- Main column: request information, comments, and history. -->
        <v-col cols="12" md="8">

          <!-- Work-order information. -->
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

                <!-- Display the supervisor approval state. -->
                <v-col cols="6">
                  <strong>Approval Status:</strong>
                  {{ formatStatus(request.approvalStatus) }}
                </v-col>

                <v-col cols="6">
                  <strong>Approved By:</strong>
                  {{ request.approvedBy?.name ?? 'Not available' }}
                </v-col>

                <v-col cols="6">
                  <strong>Approved At:</strong>
                  {{ formatDate(request.approvedAt) }}
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

              <!-- Show technician notes when available. -->
              <div v-if="request.technicianNotes" class="mt-5">
                <strong>Technician Notes:</strong>
                <p>{{ request.technicianNotes }}</p>
              </div>

              <!-- Show the resolution summary for completed work orders. -->
              <div v-if="request.resolutionSummary" class="mt-5">
                <strong>Resolution Summary:</strong>
                <p>{{ request.resolutionSummary }}</p>
              </div>
            </v-card-text>
          </v-card>

          <!-- Work-order comments and communication. -->
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

          <!-- Chronological audit trail of work-order activity. -->
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

        <!-- Right column: actions available to the current user. -->
        <v-col cols="12" md="4">

          <!-- Supervisor approval replaces manual technician assignment. -->
          <v-card
              v-if="canApprove"
              class="mb-5"
              elevation="1"
          >
            <v-card-title class="pa-5">
              Supervisor Actions
            </v-card-title>

            <v-divider />

            <v-card-text class="pa-5">

              <p class="mb-4 text-body-2">
                Approving this request will automatically assign
                it to the technician with the fewest active work orders.
              </p>

              <v-btn
                  color="primary"
                  block
                  :loading="saving"
                  :disabled="saving"
                  @click="handleApproval"
              >
                Approve Work Order
              </v-btn>

            </v-card-text>
          </v-card>

          <!-- Technician controls appear only for the assigned technician. -->
          <v-card
              v-if="isAssignedTechnician"
              elevation="1"
          >
            <v-card-title class="pa-5">
              Technician Actions
            </v-card-title>

            <v-divider />

            <v-card-text class="pa-5">

              <!-- Start an approved OPEN work order. -->
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

              <!-- Notes and estimates are editable until completion. -->
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

              <!-- Completion requires an IN_PROGRESS work order. -->
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

              <!-- Completed work orders are read-only for technicians. -->
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