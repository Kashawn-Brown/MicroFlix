// Setup helpers for k6 scenarios — login-or-register, watchlist seeding, rating seeding.
// Called from each scenario's setup() so the main VU loop starts from a known state.

import http from 'k6/http';
import { fail } from 'k6';
import { BASE_URL, TEST_USER, MOVIE_DETAIL_ID, WATCHLIST_SEED_COUNT, PATH } from './config.js';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// Get a JWT for the loadtest user. Tries login first (the common path on re-runs);
// if the user doesn't exist yet, registers and logs in.
export function loginOrRegister() {
    const loginBody = JSON.stringify({ email: TEST_USER.email, password: TEST_USER.password });

    const loginRes = http.post(`${BASE_URL}${PATH.login}`, loginBody, { headers: JSON_HEADERS });

    if (loginRes.status === 200) {
        return loginRes.json('token');
    }

    // Login failed — register and retry. Duplicate-email register returns 400,
    // which would mean state is weirder than a missing user; surface it.
    const registerBody = JSON.stringify({
        email: TEST_USER.email,
        password: TEST_USER.password,
        displayName: TEST_USER.displayName,
    });
    const registerRes = http.post(`${BASE_URL}${PATH.register}`, registerBody, { headers: JSON_HEADERS });

    if (registerRes.status !== 200) {
        fail(`register failed: status=${registerRes.status} body=${registerRes.body}`);
    }

    return registerRes.json('token');
}

// Return the first N movie ids from page 0 of the default catalog browse.
// Stable across runs as long as the catalog isn't re-ingested, and gives us
// N distinct ids so the 1+N watchlist fan-out hits different rows.
export function fetchMovieIds(count) {
    const res = http.get(`${BASE_URL}${PATH.moviesPage}?page=0&size=${count}`);
    if (res.status !== 200) {
        fail(`fetch movies page failed: status=${res.status}`);
    }
    return res.json('content').map((m) => m.id);
}

// Seed the test user's watchlist with the given movie ids. PUT is idempotent,
// so re-running setup is safe.
export function seedWatchlist(token, movieIds) {
    const authHeaders = { ...JSON_HEADERS, Authorization: `Bearer ${token}` };
    for (const id of movieIds) {
        const res = http.put(`${BASE_URL}${PATH.watchlistForId(id)}`, null, { headers: authHeaders });
        if (res.status >= 400) {
            fail(`seed watchlist failed for id=${id}: status=${res.status}`);
        }
    }
}

// Seed a rating for MOVIE_DETAIL_ID so the authed movie-detail scenario sees
// 200s on all 4 endpoints (summary has at least one rating, my-rating is set).
export function seedRating(token, movieId, rate) {
    const authHeaders = { ...JSON_HEADERS, Authorization: `Bearer ${token}` };
    const body = JSON.stringify({ movieId, rate });
    const res = http.post(`${BASE_URL}${PATH.upsertRating}`, body, { headers: authHeaders });
    if (res.status >= 400) {
        fail(`seed rating failed for id=${movieId}: status=${res.status}`);
    }
}

// Combined setup used by both scenarios. Returns { token, movieIds }.
export function setupTestUser() {
    const token = loginOrRegister();
    const movieIds = fetchMovieIds(WATCHLIST_SEED_COUNT);
    seedWatchlist(token, movieIds);
    // Ensure MOVIE_DETAIL_ID is also watchlisted and rated so the authed
    // movie-detail scenario hits 200s uniformly.
    seedWatchlist(token, [MOVIE_DETAIL_ID]);
    seedRating(token, MOVIE_DETAIL_ID, 7.5);
    return { token, movieIds };
}
