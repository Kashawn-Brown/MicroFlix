// Helps communicate safely with backend
// All calls go through the Next.js server at /gateway/... and are then
// proxied to the Spring Cloud Gateway by next.config.ts rewrites.


// Describes a possible error response from backend
export interface ProblemDetail {
    type?: string;
    title?: string;
    status?: number;
    detail?: string;
    instance?: string;
    // Backend may include extra fields like "path" – don't want to lose them
    [key: string]: unknown;
}


// Making custom type (class) for API failures
export class ApiError extends Error {
    status: number;
    problem?: ProblemDetail; // the parsed JSON error from the backend (if any)

    constructor(status: number, problem?: ProblemDetail) {

    // sets new error message, if there is a problem and it has a title, use that
    super(problem?.title ?? `Request failed with status ${status}`);  // “if the thing on the left is null or undefined, use the thing on the right instead.”
    
    this.status = status;
    this.problem = problem;
    }
}


const GATEWAY_BASE_URL = process.env.GATEWAY_BASE_URL || "http://gateway:8081";
// Use the URL from the env; otherwise, default to talk to backend running on localhost:8081
const API_PREFIX = "/gateway";


/**
 * Small wrapper around fetch for backend.
 * - Using this helper instead of directly calling fetch everywhere
 * - Prefixes with the gateway base URL
 * - Sends JSON by default
 * - Parses ProblemDetail errors and throws ApiError
 */
export async function apiFetch<T>(
    path: string,  // send in the route, e.g. '/movies' or /auth/me', etc.
    options: RequestInit = {}
): Promise<T> {

    // Ensure the path we send to fetch starts with /gateway
    const isAbsoluteUrl = /^https?:\/\//i.test(path);
    const isServer = typeof window === "undefined";


    // 1. Normalize the path to always start with '/'
    let normalizedPath = isAbsoluteUrl
    ? path
    : path.startsWith("/")
    ? path
    : `/${path}`;

    // 2. Ensure it starts with /gateway (for browser paths)
    if (!isAbsoluteUrl && !normalizedPath.startsWith(API_PREFIX)) {
        normalizedPath = `${API_PREFIX}${normalizedPath}`;
    }

    let url: string;

    // 3. Decide the final URL based on where we are:
    if (isAbsoluteUrl) {
        // Already full URL, just use it
        url = normalizedPath;
    } 
    else if (isServer) {
        // On server: call gateway directly.
        const backendPath = normalizedPath.replace(/^\/gateway/, "");
        url = new URL(backendPath, GATEWAY_BASE_URL).toString();
    } 
    else {
        // In browser: call /gateway/... on frontend, rewrites handle the rest
        url = normalizedPath;
    }

    // Calling fetch to make HTTP request
    const response = await fetch(url, {
        // We always want JSON for our APIs
        headers: {
        "Content-Type": "application/json",  // Always send "Content-Type": "application/json" headers
        ...(options.headers ?? {}), // spread any extra headers sent in
        },
        ...options,
    });

    // Reading a response
    const contentType = response.headers.get("content-type") ?? "";
    const isJson = contentType.includes("application/json");

    //Read the body accordingly: (parse if JSON, else plain text)
    const body = isJson ? await response.json() : await response.text();


    // Handle errors
    // If the response is an error (e.g., status 400, 404, 500), wrap it in ApiError and throw it
    if (!response.ok) {
        const problem: ProblemDetail | undefined = isJson ? (body as ProblemDetail) : undefined;
        throw new ApiError(response.status, problem);
    }

    // On a success, return the response body
    return body as T;
}
