<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { currentUser } from '../stores/demoUser'
import { createMaintenanceRequest } from '../services/maintenanceService'

import type {
  CreateMaintenanceRequest,
  RequestPriority,
  EquipmentCondition,
} from '../types/maintenance'

const router = useRouter()

const formValid = ref(false)
const submitting = ref(false)
const error = ref('')

const canSubmit = computed(
    () => currentUser.value.role === 'REQUESTER' ||
        currentUser.value.role === 'SUPERVISOR'
)

const form = ref<CreateMaintenanceRequest>({
  title: '',
  description: '',
  buildingNumber: '',
  priority: 'ROUTINE',
  condition: 'GOOD',
})

const priorityOptions: { title: string; value: RequestPriority }[] = [
  { title: 'Routine', value: 'ROUTINE' },
  { title: 'Urgent', value: 'URGENT' },
  { title: 'Emergency', value: 'EMERGENCY' },
]

const conditionOptions: { title: string; value: EquipmentCondition }[] = [
  { title: 'Good', value: 'GOOD' },
  { title: 'Fair', value: 'FAIR' },
  { title: 'Poor', value: 'POOR' },
]

const requiredRule = (value: string) =>
    !!value?.trim() || 'This field is required.'

async function submitRequest() {
  if (!formValid.value || !canSubmit.value) return

  submitting.value = true
  error.value = ''

  try {
    const created = await createMaintenanceRequest(
        form.value,
        currentUser.value.id
    )

    router.push(`/requests/${created.id}`)
  } catch (err) {
    error.value = err instanceof Error
        ? err.message
        : 'Failed to create maintenance request.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <div class="mb-6">
      <h1 class="text-h4 font-weight-bold">
        Create Maintenance Request
      </h1>

      <p class="text-medium-emphasis mt-1">
        Submit a new facility maintenance request.
      </p>
    </div>

    <v-alert
        v-if="!canSubmit"
        type="warning"
        variant="tonal"
        class="mb-4"
    >
      Technicians cannot submit maintenance requests.
      Switch to a requester or supervisor demo account.
    </v-alert>

    <v-alert
        v-if="error"
        type="error"
        variant="tonal"
        class="mb-4"
    >
      {{ error }}
    </v-alert>

    <v-card max-width="850" elevation="1">
      <v-card-title class="pa-5">
        Request Information
      </v-card-title>

      <v-divider />

      <v-card-text class="pa-6">
        <v-form
            v-model="formValid"
            @submit.prevent="submitRequest"
        >
          <v-text-field
              v-model="form.title"
              label="Request Title"
              variant="outlined"
              :rules="[requiredRule]"
              maxlength="150"
              counter
              class="mb-4"
          />

          <v-textarea
              v-model="form.description"
              label="Description"
              variant="outlined"
              :rules="[requiredRule]"
              rows="4"
              class="mb-4"
          />

          <v-text-field
              v-model="form.buildingNumber"
              label="Building Number / Location"
              variant="outlined"
              :rules="[requiredRule]"
              maxlength="50"
              counter
              class="mb-4"
          />

          <v-row>
            <v-col cols="12" md="6">
              <v-select
                  v-model="form.priority"
                  :items="priorityOptions"
                  label="Priority"
                  variant="outlined"
                  class="mb-4"
              />
            </v-col>

            <v-col cols="12" md="6">
              <v-select
                  v-model="form.condition"
                  :items="conditionOptions"
                  label="Building / Equipment Condition"
                  variant="outlined"
                  class="mb-4"
              />
            </v-col>
          </v-row>

          <v-divider class="mb-5" />

          <div class="d-flex justify-end ga-3">
            <v-btn
                variant="outlined"
                @click="router.push('/requests')"
            >
              Cancel
            </v-btn>

            <v-btn
                type="submit"
                color="primary"
                :loading="submitting"
                :disabled="!formValid || !canSubmit"
                prepend-icon="mdi-content-save"
            >
              Submit Request
            </v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>