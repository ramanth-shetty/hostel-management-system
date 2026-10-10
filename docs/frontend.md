# Hostel Management System Frontend Specification (Frontend.md)

## 1. Structure

No build step. Everything is served by Spring Boot from `src/main/resources/static/`:

| File                               | Role                                       |
| ---------------------------------- | ------------------------------------------ |
| `index.html`                       | Layout, styling, all modals                |
| `app.js`                           | All behaviour: API calls, rendering, forms |
| `error/404.html`, `error/500.html` | Standalone error pages                     |

Libraries (CDN): Tailwind (Play CDN), FontAwesome 6, Google Font **Plus Jakarta Sans**. Vanilla ES6 JavaScript, no framework.

## 2. Design System

**Palette** (from the Lotly reference screenshot)

| Use                                 | Value                                                      |
| ----------------------------------- | ---------------------------------------------------------- |
| Primary / active nav / main buttons | Orange `#FF9500` (hover `#F28500`); tint `#FFF7ED`         |
| Count badge                         | Purple `#4F46E5`                                           |
| Money: collected / pending          | Emerald / rose text and soft tinted icon tiles             |
| Surfaces                            | White, borders `gray-100`, dividers `gray-50`, `shadow-sm` |

**Typography:** Plus Jakarta Sans. Page title 24px extrabold uppercase; table content 13px; labels 12px `gray-500`. Hierarchy comes from weight and gray tones, not pure black or heavy bold.

**Reusable classes** (defined in `index.html`): `.field` (inputs/selects), `.label`, `.btn-primary`, `.btn-ghost`, `.nav-item` (+ `.active`), `.card`. Prefer these over repeating long utility strings.

**Status badges:** soft translucent background with slightly darker text: green for `ACTIVE`/`AVAILABLE`, gray for `CHECKED_OUT`, red for `FULL`.

> Coupling to be aware of: `app.js` builds table rows, room cards and toasts with Tailwind classes. The badge, row-button and toast restyling is done by CSS override rules in the second `<style>` block of `index.html` that target those generated elements (for example `#student-table-body span.rounded-full.bg-emerald-100`). If you change the classes in `app.js`, update those rules too.

## 3. Layout

- **Desktop (`lg`+):** fixed left sidebar (Residents with purple active-count badge, Rooms, Monthly fee with current value, H2 Console, Logout). Content area: page title, then a 12-column grid:
  - Left (4 cols): resident form card.
  - Right (8 cols): three stat cards, then the resident directory table (the table scrolls inside its card).
- **Mobile:** sidebar becomes a horizontally scrollable top bar; the page scrolls normally; the table scrolls horizontally; a floating "Register resident" button scrolls to and focuses the form.

## 4. Screens and Modals

| Element id      | Purpose                                                   |
| --------------- | --------------------------------------------------------- |
| `student-form`  | Register / edit resident                                  |
| `action-modal`  | Confirm check-out                                         |
| `payment-modal` | Record a payment and view that resident's payment history |
| `rooms-modal`   | Room cards: occupancy `n / 4`, status, resident names     |
| `fee-modal`     | View and change the standard monthly fee                  |

All modals share the class `modal-overlay` and close with the X/Cancel buttons (`data-close-modal`), a click on the backdrop, or the Escape key.

Row actions in the table (`data-action`): `pay` (record payment), `edit`, `checkout`. Edit and checkout are disabled for `CHECKED_OUT` residents; `pay` is always available.

## 5. Data Flow and State

- `state` holds: `students`, `pendingOnly`, `query`, plus the ids of the resident currently being checked out or paid.
- `fetchStudents()` always loads the **full** list from `GET /api/students`. The search box and the "Pending dues only" toggle only **filter on screen** (`getVisibleStudents()`), so the stat cards always reflect everyone.
- Stat cards: Active residents = count with status `ACTIVE`; Collected and Pending = sums of the server-provided `feesPaid` / `pendingFees`.
- **The frontend never calculates fees.** It displays server values only. After any payment, fee change, or check-out it re-fetches.

## 6. API Client (`apiRequest`)

- Adds `Accept: application/json`, and for non-GET requests copies the `XSRF-TOKEN` cookie into the `X-XSRF-TOKEN` header.
- A 401 (or a redirect to `/login`) sends the browser to `/login`.
- A 403 shows "Security token missing or expired. Refresh the page and try again."
- Any other error throws an `Error` carrying `message` and `fieldErrors` from the backend error body.

## 7. Forms and Validation

- **Create mode:** name, room number, admission date, initial payment (optional), payment mode.
- **Edit mode:** the initial-payment block is hidden. Fees are changed only through the payment modal. Cancel returns to create mode.
- Room number is uppercased as you type and checked against `^[A-Z]-[1-9][0-9]{2}$` on blur and on submit. The request is not sent if it fails.
- Server validation errors (`fieldErrors`) are shown under the matching input. The rule: **the key in `fieldErrors` must equal the input's `id` and the `data-error-for` value** (`name`, `roomNumber`, `admissionDate`, `initialPayment`, `paymentMode`).
- Payment and fee modals show errors in their own inline lines (`pay-error`, `fee-error`).

## 8. UX Behaviour

- Toasts (top right, auto-dismiss after 4.5 s) replace `alert()`. Success is green, error is red.
- Skeleton rows while the table loads; an empty-state message when nothing matches.
- Submit buttons show a spinner and are disabled during a request, then restore their original label.
- "Pending dues only" button gets a ring when active (`aria-pressed` is kept in sync).
- Logout submits a hidden form to `POST /logout` with the CSRF token.

## 9. Rules for Developers

1. Every `id` used in `app.js` must exist in `index.html` (and vice versa when renaming). Removing one breaks the page silently.
2. Escape all dynamic text with `escapeHtml()` before putting it in `innerHTML`.
3. Never compute money or room occupancy in JS.
4. To add a field to the resident form: add the input with matching `id`, add a `data-error-for` line, add it to the payload in `handleFormSubmit`, add it to the backend DTO with the same name, then show it in `renderTable`.
5. Money is formatted with `formatCurrency()` (`₹` + 2 decimals).
6. The sidebar active-count badge is kept in sync by a small inline script at the bottom of `index.html` that mirrors `#stat-total`.
