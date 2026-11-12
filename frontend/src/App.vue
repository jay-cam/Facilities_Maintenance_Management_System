<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  currentUser,
  demoUsers,
  switchUser,
} from './stores/demoUser'


// --------------------------------------------------
// Application Navigation
// --------------------------------------------------

/**
 * Provides access to Vue Router.
 *
 * The router controls navigation between the dashboard,
 * work-order list, new-request form, and request details.
 */
const router = useRouter()

/**
 * Controls whether the sidebar navigation drawer is open.
 *
 * Vue's ref() makes this value reactive.
 * The drawer starts in the open position.
 */
const drawer = ref(true)


// --------------------------------------------------
// Demo User Selection
// --------------------------------------------------

/**
 * Connects the user dropdown to the currently selected
 * demo user stored in demoUser.ts.
 *
 * The getter retrieves the current user's ID.
 *
 * The setter calls switchUser() when someone selects
 * a different user from the dropdown.
 *
 * Vue's computed() keeps the dropdown synchronized
 * with the application's reactive user state.
 */
const selectedUser = computed({

  get: () => currentUser.value.id,

  set: (value: number) => switchUser(value),

})

/**
 * Formats the currently selected user's information.
 *
 * Example:
 * John Smith — REQUESTER
 * Waterfront Operations
 *
 * This information appears in the selected-user field.
 */
const selectedUserLabel = computed(() => {
  const user = currentUser.value

  return `${user.name} — ${user.role}`
})


// --------------------------------------------------
// Sidebar Navigation Items
// --------------------------------------------------

/**
 * Defines the application's primary navigation links.
 *
 * Each item contains:
 * - A display title.
 * - A Material Design icon.
 * - A Vue Router destination.
 *
 * Vue renders these items dynamically using v-for.
 */
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

    <!--
      Main application sidebar.

      The drawer provides navigation between the
      application's primary pages.
    -->
    <v-navigation-drawer
        v-model="drawer"
        color="primary"
    >

      <!-- Application branding. -->
      <div class="pa-5">
        <div class="text-h6 font-weight-bold">
          Facilities Management
        </div>

        <div class="text-caption mt-1">
          Work Order Management System
        </div>
      </div>

      <v-divider />

      <!--
        Render navigation links from the navigation array.

        Clicking an item uses Vue Router to navigate
        without reloading the entire application.
      -->
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

      <!-- Sidebar footer identifying the demonstration. -->
      <template #append>
        <div class="pa-4 text-caption">
          NAVFAC Technical Demonstration
        </div>
      </template>

    </v-navigation-drawer>


    <!-- --------------------------------------------------
         Application Header
         -------------------------------------------------- -->

    <v-app-bar
        elevation="1"
        color="surface"
    >

      <!-- Toggle the navigation drawer. -->
      <template #prepend>
        <v-app-bar-nav-icon
            @click="drawer = !drawer"
        />
      </template>

      <v-app-bar-title>
        Facilities Maintenance
      </v-app-bar-title>

      <v-spacer />

      <!--
        Indicates that this application uses demonstration
        accounts rather than production authentication.
      -->
      <v-chip
          color="warning"
          variant="tonal"
          class="mr-4"
      >
        DEMO MODE
      </v-chip>


      <!-- --------------------------------------------------
           Demo User Selector
           -------------------------------------------------- -->

      <!--
        Allows switching between all nine demo users.

        Each dropdown option displays:
        - User name
        - Application role
        - Department

        Changing the selection updates currentUser
        through the selectedUser computed property.
      -->
      <v-select
          v-model="selectedUser"
          :items="demoUsers"
          item-title="name"
          item-value="id"
          label="Current User"
          variant="outlined"
          density="compact"
          hide-details
          style="width: 310px; max-width: 310px"
          class="mr-5"
      >

        <!--
          Customize each option in the demo-user dropdown.

          Vuetify provides the user object through the item slot.
          The user contains the name, role, and department
          defined in demoUser.ts.
        -->
        <template #item="{ props, item }">
          <v-list-item
              v-bind="props"
              :title="item.name"
              :subtitle="`${item.role} — ${item.department}`"
          />
        </template>

        <!--
          Customize how the currently selected user
          appears when the dropdown is closed.
        -->
        <template #selection>
          <div class="d-flex flex-column justify-center">

            <span
                class="text-body-2 font-weight-medium"
                style="line-height: 1.2"
            >
              {{ selectedUserLabel }}
            </span>

            <span
                class="text-caption text-medium-emphasis"
                style="line-height: 1.2"
            >
              {{ currentUser.department }}
            </span>

          </div>
        </template>

      </v-select>

    </v-app-bar>


    <!-- --------------------------------------------------
         Main Application Content
         -------------------------------------------------- -->

    <v-main>

      <v-container
          fluid
          class="pa-6"
      >

        <!--
          Vue Router renders the current page here.

          The key is tied to the current user's ID.

          When the demo user changes, Vue recreates
          the current page component.

          This causes its onMounted() logic to run again,
          allowing the page to retrieve data appropriate
          for the newly selected user.

          Example:
          Switching from John Smith to Sarah Mitchell
          causes the dashboard to reload using Sarah's
          department-specific permissions.
        -->
        <router-view :key="currentUser.id" />

      </v-container>

    </v-main>

  </v-app>
</template>