// Baseline k6 scenario — authenticated watchlist page load (true 1+N).
//
// Mirrors app/watchlist/page.tsx:
//   1. GET /rating-service/api/v1/engagements/watchlist
//   2. Promise.all(engagements.map(eng => GET /movie-service/api/v1/movies/{id}))
//
// k6 reproduces (2) via http.batch so the N fan-out fires in parallel, matching
// what the browser does. Records a custom per-page-load Trend alongside k6's
// per-request metrics so docs/benchmarks.md gets a "page feels slow / feels fast"
// number, not just per-endpoint latency.

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, PATH } from '../lib/config.js';
import { setupTestUser } from '../lib/setup.js';

const pageLoadDuration = new Trend('page_load_duration', true);

export const options = {
    scenarios: {
        watchlist: {
            executor: 'constant-arrival-rate',
            rate: 5,
            timeUnit: '1s',
            duration: '60s',
            preAllocatedVUs: 10,
            maxVUs: 20,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    return setupTestUser();
}

export default function (data) {
    const { token } = data;
    const authHeaders = { Authorization: `Bearer ${token}` };

    const pageStart = Date.now();

    // Step 1 — fetch the engagement list.
    const engagementsRes = http.get(`${BASE_URL}${PATH.watchlist}`, { headers: authHeaders });
    check(engagementsRes, {
        'watchlist 200': (r) => r.status === 200,
    });

    const engagements = engagementsRes.json();

    // Step 2 — parallel fan-out to movie-service, one request per watchlist entry.
    // http.batch fires all requests concurrently, like the browser's Promise.all.
    if (engagements && engagements.length > 0) {
        const movieRequests = engagements.map((eng) => ({
            method: 'GET',
            url: `${BASE_URL}${PATH.movieById(eng.movieId)}`,
        }));
        const movieResponses = http.batch(movieRequests);

        const allOk = movieResponses.every((r) => r.status === 200);
        check(null, { 'all movie fetches 200': () => allOk });
    }

    pageLoadDuration.add(Date.now() - pageStart);
}
