// Handle backend auth routes

import { apiFetch } from "./api-client";

// Expected response when login/register succeeds
export type AuthResponse = {
    token: string;
    email: string;
    displayName: string;
    roles: string;
};

// Defining how to send login request
export type LoginRequest = {
    email: string;
    password: string;
};

// Defining how to send register request
export type RegisterRequest = {
    email: string;
    password: string;
    displayName: string;
};


/**
 * Call the user-service login endpoint via the gateway.
 */
export async function login(request: LoginRequest): Promise<AuthResponse> {

    return apiFetch<AuthResponse>("/user-service/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify(request),
    });
}


/**
 * Call the user-service register endpoint via the gateway.
 */
export async function register(request: RegisterRequest): Promise<AuthResponse> {

    return apiFetch<AuthResponse>("/user-service/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify(request),
    });
}
