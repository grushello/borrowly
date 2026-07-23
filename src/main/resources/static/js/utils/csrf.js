// Shared CSRF helper for cookie-authenticated requests.
//
// Spring exposes the CSRF token in a readable "XSRF-TOKEN" cookie. For any
// state-changing request (POST/PUT/PATCH/DELETE) we read that cookie and echo
// it back in the "X-XSRF-TOKEN" header, which Spring validates.
//
// RULE: use csrfFetch() for any POST/PUT/PATCH/DELETE.
// Plain fetch() on those will be rejected with 403.

let SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

function readCookie(name) {
    const prefix = `${name}=`;
    const parts = document.cookie ? document.cookie.split('; ') : [];
    const match = parts.find(part => part.startsWith(prefix));
    return match ? decodeURIComponent(match.substring(prefix.length)) : null;
}

function csrfFetch(input, init = {}) {
    const method = (init.method || 'GET').toUpperCase();

    init = { credentials: 'same-origin', ...init };

    if (!SAFE_METHODS.has(method)) {
        const token = readCookie('XSRF-TOKEN');
        if (!token) {
            console.error('csrfFetch: XSRF-TOKEN cookie missing; request not sent.');
            return Promise.reject(new Error('Missing CSRF token'));
        }
        const headers = new Headers(init.headers || {});
        if (!headers.has('X-XSRF-TOKEN')) {
            headers.set('X-XSRF-TOKEN', token);
        }
        init = { ...init, headers };
    }
    return fetch(input, init);
}