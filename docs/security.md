# Hostel Management System Security Specification (Security.md)

## 1. Authentication

- Spring Security 6 form login with a server-side session (`JSESSIONID`).
- One admin account, built at startup from `HOSTEL_ADMIN_USERNAME` / `HOSTEL_ADMIN_PASSWORD` and kept in memory. Role: `ADMIN`.
- Passwords are hashed with `BCryptPasswordEncoder(12)`. No plaintext passwords in code, config, or database.
- Login page: Spring's built-in `/login`. On success the user lands on `/`.
- Logout: `POST /logout` (needs CSRF token). It invalidates the session, deletes `JSESSIONID`, and redirects to `/login?logout`. The UI Logout button submits a form carrying the token.

## 2. Authorization

| Path                                                                               | Access        |
| ---------------------------------------------------------------------------------- | ------------- |
| `/login`, `/css/**`, `/js/**`, `/images/**`, `/404`, `/500`, `/error`, `/error/**` | Public        |
| `/h2-console/**`                                                                   | `ADMIN` only  |
| `/api/students/**`, `/api/rooms/**`, `/api/payments/**`, `/api/settings/**`        | `ADMIN` only  |
| Everything else (including `/`, `index.html`, `app.js`)                            | Authenticated |

Notes:

- Unauthenticated `/api/**` calls get **HTTP 401** (not a redirect to the HTML login page). The frontend reacts by sending the user to `/login`.
- Path rules use explicit Ant matchers. This avoids Spring Security's "MVC or not?" startup error that appears when the H2 console servlet is present.

## 3. CSRF Protection

Enabled for all state-changing requests (POST, PUT, DELETE), except `/h2-console/**` (the console has its own login form).

How it works with the JavaScript frontend:

1. Spring stores the token in a **readable cookie** named `XSRF-TOKEN`.
2. `CsrfCookieFilter` reads the token on every request. Spring Security 6 otherwise delays creating it, which made the first POST fail.
3. `app.js` copies the cookie into the `X-XSRF-TOKEN` header on every non-GET fetch.
4. The plain (non-XOR) token handler is used so the raw cookie value is accepted.
5. A 403 on a write call normally means the page is stale: refresh and retry. The UI shows a message saying so.

## 4. Input and Output Protection

- **Validation:** Jakarta Bean Validation on every request DTO (`@Valid`), plus the same room-number rule in the entity and service.
- **XSS:** every dynamic value inserted into the DOM goes through `escapeHtml()` (table rows, modals, toasts, room cards, payment history). Server responses are JSON only.
- **SQL injection:** only Spring Data derived queries and parameterised JPQL (`@Param`). No string-built SQL.
- **Data integrity under concurrency:** room rows are locked during registration or moves so capacity cannot be exceeded by simultaneous requests.
- **Mass-assignment:** fee fields are not part of any request DTO, so a client cannot set `feesPaid` or `pendingFees`.

## 5. Secrets and Data

- Admin credentials and the fee seed come from `.env` or OS environment variables only. `.env` and `data/` are in `.gitignore`.
- `.env.example` documents the variable names with placeholder values only.
- The H2 database file lives in `./data/` and is not encrypted. Protect the machine and back up `data/` as needed.

## 6. Error Exposure

- Clients never receive stack traces: `server.error.include-stacktrace=never`, `server.error.include-message=never`.
- Unexpected exceptions return a generic message; the full cause is logged on the server.
- Custom static pages exist for `404` and `500` (`static/error/404.html`, `500.html`).

## 7. Headers and Cookies

- Spring Security default headers are on (`X-Content-Type-Options`, cache control, etc.).
- `X-Frame-Options: SAMEORIGIN` (needed for the H2 console).
- Session cookie: `SameSite=Lax`.
- H2 console: `web-allow-others=false` (localhost only).

## 8. Before Going to Production

These are **not** done in the current local setup:

1. Serve over HTTPS and set `server.servlet.session.cookie.secure=true`.
2. Disable the H2 console (`spring.h2.console.enabled=false`) and move to a server database (PostgreSQL/MySQL).
3. Use a strong admin password; consider a login rate limit or lockout.
4. Replace CDN scripts (Tailwind, FontAwesome) with self-hosted or SRI-pinned copies and add a Content-Security-Policy.
5. Keep secrets in the hosting platform's secret store rather than a file.
