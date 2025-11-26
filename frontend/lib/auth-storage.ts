// Helpers for storing, loading, and clearing auth info (JWT + user identity) in localStorage

import type { AuthResponse } from "./auth-api";

const STORAGE_KEY = "microflix_auth";

/**
 * Save auth info (including JWT) to localStorage.
 * MVP approach: simple and easy to debug.
 */
export function saveAuth(auth: AuthResponse) {
    if (typeof window === "undefined") return;
    
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    } catch {
        // Swallow errors – storage is a convenience, not critical path
    }
}

/**
 * Load auth info from localStorage.
 */
export function loadAuth(): AuthResponse | null {
  
    if (typeof window === "undefined") return null;
  
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return null;
        return JSON.parse(raw) as AuthResponse;
    } catch {
        return null;
    }
}

/**
 * Clear stored auth info (logout).
 */
export function clearAuth() {
    if (typeof window === "undefined") return;
    
    try {
        localStorage.removeItem(STORAGE_KEY);
    } catch {
        // ignore
    }
}
