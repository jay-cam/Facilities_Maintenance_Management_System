import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'

import { createVuetify } from 'vuetify'
import { aliases, mdi } from 'vuetify/iconsets/mdi'

export default createVuetify({
    theme: {
        defaultTheme: 'light',
        themes: {
            light: {
                dark: false,
                colors: {
                    primary: '#183153',
                    secondary: '#32689B',
                    background: '#F5F7FA',
                    surface: '#FFFFFF',
                    success: '#2E7D32',
                    warning: '#ED6C02',
                    error: '#D32F2F',
                },
            },
        },
    },
    icons: {
        defaultSet: 'mdi',
        aliases,
        sets: { mdi },
    },
})