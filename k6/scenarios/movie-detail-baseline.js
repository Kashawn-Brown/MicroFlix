// Baseline k6 scenario — authenticated movie-detail page load (4 concurrent fetches).
//
// Mirrors app/movies/[id]/page.tsx + components/movie-actions.tsx:
//   SSR:   GET /movie-service/api/v1/movies/{id}
//          GET /rating-service/api/v1/ratings/movie/{id}/summary
//   CSR:   GET /rating-service/api/v1/ratings/movie/{id}/me          (auth)
//          GET /rating-service/api/v1/engagements/watchlist/{id}/me  (auth)
//
// From the user's perspective these 4 all happen at roughly the same time, so
// k6 fires them in a single http.batch. setup() seeds a rating and a watchlist
// entry for MOVIE_DETAIL_ID so all 4 endpoints return 200 under the loadtest
// user (summary has a rating, my-rating is set, in-watchlist is true).
//
// Anonymous path (2 fetches) is the trivial subset; the authed path is the
// harder one and the one that will regress / improve when Piece 2 migrates the
// page onto the gateway aggregation endpoint.

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
        { method: 'GET', url: `${BASE_URL}${PATH.movieById(MOVIE_DETAIL_ID)}` },
        { method: 'GET', url: `${BASE_URL}${PATH.ratingSummary(MOVIE_DETAIL_ID)}` },
        { method: 'GET', url: `${BASE_URL}${PATH.myRating(MOVIE_DETAIL_ID)}`, params: { headers: authHeaders } },
        { method: 'GET', url: `${BASE_URL}${PATH.inWatchlist(MOVIE_DETAIL_ID)}`, params: { headers: authHeaders } },
    ]);

    check(responses[0], { 'movie 200':   (r) => r.status === 200 });
    check(responses[1], { 'summary 200': (r) => r.status === 200 });
    check(responses[2], { 'my-rating 200': (r) => r.status === 200 });
    check(responses[3], { 'in-watchlist 200': (r) => r.status === 200 });

    pageLoadDuration.add(Date.now() - pageStart);
}
