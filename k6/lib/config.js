// Shared config for k6 load tests — endpoints, test identities, fixed movie id.

// Default targets the gateway container on the docker_default compose network.
// Override with env BASE_URL=http://localhost:8081 when running k6 from the host
// outside the compose network.
export const BASE_URL = __ENV.BASE_URL || 'http://gateway:8081';

// Dedicated loadtest identity; recreated on first run via register-or-login in setup().
export const TEST_USER = {
    email: 'loadtest@microflix.local',
    password: 'loadtest-password-1',
    displayName: 'loadtest',
};

// Fixed movie id for movie-detail baseline, matching the Q6/Q8 reference id in
// docs/explain-analyze.md. Hardcoding keeps runs reproducible; realistic traffic
// would vary the id across iterations.
export const MOVIE_DETAIL_ID = 524;

// How many movies to seed into the test user's watchlist. The watchlist
// scenario's 1+N shape exercises N distinct movie rows per iteration.
export const WATCHLIST_SEED_COUNT = 10;

// Backend paths via the gateway. apiFetch in the frontend prepends /gateway;
// k6 calls the gateway directly, so no prefix.
export const PATH = {
    login:           '/user-service/api/v1/auth/login',
    register:        '/user-service/api/v1/auth/register',
    moviesPage:      '/movie-service/api/v1/movies',
    movieById:       (id) => `/movie-service/api/v1/movies/${id}`,
    ratingSummary:   (id) => `/rating-service/api/v1/ratings/movie/${id}/summary`,
    myRating:        (id) => `/rating-service/api/v1/ratings/movie/${id}/me`,
    upsertRating:    '/rating-service/api/v1/ratings',
    watchlist:       '/rating-service/api/v1/engagements/watchlist',
    watchlistForId:  (id) => `/rating-service/api/v1/engagements/watchlist/${id}`,
    inWatchlist:     (id) => `/rating-service/api/v1/engagements/watchlist/${id}/me`,
};
