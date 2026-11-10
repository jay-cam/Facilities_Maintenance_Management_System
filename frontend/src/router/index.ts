import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'dashboard',
            component: () => import('../views/DashboardView.vue'),
        },
        {
            path: '/requests',
            name: 'requests',
            component: () => import('../views/RequestsView.vue'),
        },
        {
            path: '/requests/new',
            name: 'new-request',
            component: () => import('../views/CreateRequestView.vue'),
        },
        {
            path: '/requests/:id',
            name: 'request-details',
            component: () => import('../views/RequestDetailsView.vue'),
        },
    ],
})

export default router