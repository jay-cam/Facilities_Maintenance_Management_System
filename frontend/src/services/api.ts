const API_BASE = '/api'

export class ApiError extends Error {
    public status: number

    constructor(status: number, message: string) {
        super(message)
        this.name = 'ApiError'
        this.status = status
    }
}

export async function apiRequest<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<T> {
    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers: {
            ...options.headers,
        },
    })

    if (!response.ok) {
        let message = `Request failed (${response.status})`

        try {
            const error = await response.json()
            message = error.message || message
        } catch {
            // Use the default error message.
        }

        throw new ApiError(response.status, message)
    }

    if (response.status === 204) {
        return undefined as T
    }

    return response.json() as Promise<T>
}