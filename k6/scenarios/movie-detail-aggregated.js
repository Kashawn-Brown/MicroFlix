// Post-migration k6 scenario — authenticated movie-detail page load via the
// gateway's aggregation endpoint.
//
// Mirrors the migrated app/movies/[id]/page.tsx + components/movie-actions.tsx:
//   SSR:   GET /api/v1/catalog/movies/{id}                 (no auth → me is anonymous)
//   CSR:   GET /api/v1/catalog/movies/{id}                 (authed → me filled in)
//
// Two calls per page load instead of the baseline's four, both landing at the
// same aggregation endpoint but with different auth headers — the gateway
// short-circuits the me section server-side when no Authorization is present.
//
// Load shape matches movie-detail-baseline.js (20 iter/sec, 60s) so the median-of-3
// before/after comparison is apples to apples.
//
// Note on http.batch: same caveat as the baseline — firing the SSR-analogue and
// CSR-analogue requests in parallel over-models what the real browser does
// (SSR lands before CSR can even start). Keeping the batch here preserves
// like-for-like comparison against the baseline; docs/benchmarks.md covers the
// caveat explicitly.

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, MOVIE_DETAIL_ID, PATH } from '../lib/config.js';
import { setupTestUser } from '../lib/setup.js';

const pageLoadDuration = new Trend('page_load_duration', true);

export const options = {
    scenarios: {
        movieDetail: {
            executor: 'constant-arrival-rate',
            rate: 20,
            timeUnit: '1s',
            duration: '60s',
            preAllocatedVUs: 20,
            maxVUs: 50,
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

    const responses = http.batch([
        { method: 'GET', url: `${BASE_URL}${PATH.catalogMovie(MOVIE_DETAIL_ID)}` },
        { method: 'GET', url: `${BASE_URL}${PATH.catalogMovie(MOVIE_DETAIL_ID)}`, params: { headers: authHeaders } },
    ]);

    check(responses[0], { 'catalog anon 200': (r) => r.status === 200 });
    check(responses[1], { 'catalog authed 200': (r) => r.status === 200 });

    pageLoadDuration.add(Date.now() - pageStart);
}
