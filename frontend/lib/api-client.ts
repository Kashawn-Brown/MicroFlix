// Centralized API helper for talking to the backend safely
// Works both in the browser and during server-side rendering.


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
    super(problem?.title ?? problem?.detail ?? `Request failed with status ${status}`);  // “if the thing on the left is null or undefined, use the thing on the right instead.”
    
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
    }
}



// For server-side fetches, we talk directly to the gateway URL.
// In Docker on EC2: GATEWAY_BASE_URL=http://gateway:8081
// In bare dev (npm run dev): GATEWAY_BASE_URL=http://localhost:8081
const GATEWAY_BASE_URL = process.env.GATEWAY_BASE_URL || "http://gateway:8081";

// All backend calls *logically* go under this prefix on the frontend.
// In the browser, the URL looks like: /gateway/...
// Next.js rewrites those to the real gateway URL (see next.config.ts).
// Use the URL from the env; otherwise, default to talk to backend running on localhost:8081
const API_PREFIX = "/gateway";


function buildUrl(path: string): string {
  const isAbsolute = /^https?:\/\//i.test(path);

  if (isAbsolute) {
    return path;
  }

  if (path.startsWith("/gateway")) {
    return path;
  }

  // Normal case: caller passes "/user-service/..." or "/movie-service/..."
  return `${API_PREFIX}${path}`;
}

/**
 * Small wrapper around fetch for backend calls.
 *
 * Usage from code:
 *   apiFetch("/movie-service/api/v1/movies")
 *   apiFetch("/rating-service/api/v1/ratings", { method: "POST", body: ... })
 */
export async function apiFetch<T>(
    path: string,  // send in the route, e.g. '/movies' or /auth/me', etc.
    options: RequestInit = {}
): Promise<T> {

    const url = buildUrl(path);

    // Basic logging while we debug
    if (typeof window === "undefined") {
        console.log("apiFetch[server] ->", url);
    } 
    else {
        console.log("apiFetch[browser] ->", url);
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
        console.error("[apiFetch] error", {
            url: url,
            status: response.status,
            body,
        });
        const problem: ProblemDetail | undefined = isJson ? (body as ProblemDetail) : undefined;
        throw new ApiError(response.status, problem);
    }

    // On a success, return the response body
    return body as T;
}
