// Post-migration k6 scenario — authenticated watchlist page load via the
// gateway's aggregation endpoint.
//
// Mirrors the migrated app/watchlist/page.tsx:
//   GET /api/v1/catalog/watchlist                          (authed, already joined)
//
// One request per page load instead of the baseline's 1 + N (11 in our seeded
// case). The gateway fans out to rating-service (engagements) and movie-service
// (batch hydration) internally and returns the joined list in addedAt-desc order.
//
// Load shape matches watchlist-baseline.js (5 iter/sec, 60s) for apples-to-apples
// median-of-3 comparison. Offered request rate is therefore 5 req/sec here vs
// ~60 req/sec for the baseline — that's the point: the migration collapses
// 12 client-initiated requests into 1, and the gateway absorbs the fan-out.

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

    const res = http.get(`${BASE_URL}${PATH.catalogWatchlist}`, { headers: authHeaders });
    check(res, { 'catalog watchlist 200': (r) => r.status === 200 });

    pageLoadDuration.add(Date.now() - pageStart);
}
