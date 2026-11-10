<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  currentUser,
  demoUsers,
  switchUser,
} from './stores/demoUser'

const router = useRouter()
const drawer = ref(true)

const selectedUser = computed({
  get: () => currentUser.value.id,
  set: (value: number) => switchUser(value),
})

const navigation = [
  {
    title: 'Dashboard',
    icon: 'mdi-view-dashboard-outline',
    route: '/',
  },
  {
    title: 'Work Orders',
    icon: 'mdi-clipboard-text-outline',
    route: '/requests',
  },
  {
    title: 'New Request',
    icon: 'mdi-plus-circle-outline',
    route: '/requests/new',
  },
]
</script>

<template>
  <v-app>
    <v-navigation-drawer v-model="drawer" color="primary">
      <div class="pa-5">
        <div class="text-h6 font-weight-bold">
          Facilities Management
        </div>
        <div class="text-caption mt-1">
          Work Order Management System
        </div>
      </div>

      <v-divider />

      <v-list nav class="mt-3">
        <v-list-item
            v-for="item in navigation"
            :key="item.route"
            :title="item.title"
            :prepend-icon="item.icon"
            :active="router.currentRoute.value.path === item.route"
            @click="router.push(item.route)"
        />
      </v-list>

      <template #append>
        <div class="pa-4 text-caption">
          NAVFAC Technical Demonstration
        </div>
      </template>
    </v-navigation-drawer>

    <v-app-bar elevation="1" color="surface">
      <template #prepend>
        <v-app-bar-nav-icon @click="drawer = !drawer" />
      </template>

      <v-app-bar-title>
        Facilities Maintenance
      </v-app-bar-title>

      <v-spacer />

      <v-chip color="warning" variant="tonal" class="mr-4">
        DEMO MODE
      </v-chip>

      <v-select
          v-model="selectedUser"
          :items="demoUsers"
          item-title="name"
          item-value="id"
          label="Current User"
          variant="outlined"
          density="compact"
          hide-details
          style="max-width: 220px"
          class="mr-5"
      >
        <template #item="{ props, item }">
          <v-list-item
              v-bind="props"
              :subtitle="item.role"
          />
        </template>
      </v-select>
    </v-app-bar>

    <v-main>
      <v-container fluid class="pa-6">
        <router-view :key="currentUser.id" />
      </v-container>
    </v-main>
  </v-app>
</template>