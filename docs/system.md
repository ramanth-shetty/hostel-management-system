# Hostel Management System Specification (System.md)

## 1. Overview

A single-tenant admin dashboard for one Hostel Owner/Warden. It covers resident onboarding, room allocation (max 4 per room), payment logging, and automatic fee calculation. The backend is the single source of truth for all money and occupancy numbers; the browser only displays them.

## 2. Tech Stack

| Layer              | Choice                                                                    |
| ------------------ | ------------------------------------------------------------------------- |
| Language / runtime | Java 21                                                                   |
| Framework          | Spring Boot 3.4.3 (Web, Data JPA, Validation, Security)                   |
| Database           | H2 file DB: `jdbc:h2:file:./data/hosteldb;AUTO_SERVER=TRUE`               |
| Schema management  | Hibernate `ddl-auto=update`                                               |
| Frontend           | Static HTML + vanilla JS + Tailwind (CDN) + FontAwesome (see Frontend.md) |

## 3. Configuration

Secrets are never hard-coded. They come from a local `.env` file in the project root (loaded by `spring.config.import=optional:file:.env[.properties]`) or from real OS environment variables.

| Variable                      | Required            | Purpose                                                                                                                              |
| ----------------------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `HOSTEL_ADMIN_USERNAME`       | Yes                 | Admin login name                                                                                                                     |
| `HOSTEL_ADMIN_PASSWORD`       | Yes                 | Admin password (BCrypt-hashed in memory at startup)                                                                                  |
| `HOSTEL_STANDARD_MONTHLY_FEE` | No (default `0.00`) | **Seed value only.** Used the first time the app starts. After that the fee lives in the database and is changed through the UI/API. |

The app refuses to start if the two admin variables are missing. `.env` and `data/` must never be committed.

**Schema changes:** `ddl-auto=update` adds tables and columns but never drops or alters old ones. If the entity model changes in an incompatible way, stop the app and delete the `data/` folder (this wipes local data).

## 4. Code Layout (`com.hostel`)

| Package      | Contents                                                                                                  |
| ------------ | --------------------------------------------------------------------------------------------------------- |
| `config`     | `SecurityConfig`                                                                                          |
| `controller` | `StudentController`, `PaymentController`, `RoomController`, `SettingController`, `GlobalExceptionHandler` |
| `service`    | `StudentService`, `PaymentService`, `RoomService`, `SettingService`                                       |
| `repository` | JPA repositories for the four entities                                                                    |
| `entity`     | `Student`, `Room`, `PaymentRecord`, `SystemSetting`                                                       |
| `dto`        | Java records for every request/response body                                                              |
| `exception`  | `ResourceNotFoundException`, `RoomFullException`                                                          |

Rule: controllers only validate and delegate; business rules live in services; entities are never returned to the client, only DTOs.

## 5. Domain Model

| Entity          | Fields                                                       | Notes                                                                               |
| --------------- | ------------------------------------------------------------ | ----------------------------------------------------------------------------------- |
| `Student`       | `id` (Long), `name`, `roomNumber`, `admissionDate`, `status` | `status` is `ACTIVE` or `CHECKED_OUT`. **No fee columns**: fees are always derived. |
| `Room`          | `roomNumber` (PK), `capacity` (always 4), `status`           | `status` is `AVAILABLE` or `FULL`. Occupancy is computed, not stored.               |
| `PaymentRecord` | `id`, `studentId`, `amount`, `paymentDate`, `paymentMode`    | Append-only audit trail. Mode: `CASH`, `UPI`, `CARD`, `BANK_TRANSFER`.              |
| `SystemSetting` | `settingName` (unique), `settingValue`                       | Holds `standard_monthly_fee`.                                                       |

## 6. Business Rules

### 6.1 Fees (computed on the backend only)

- `feesPaid` = sum of the student's `PaymentRecord.amount`
- `pendingFees` = `max(standard_monthly_fee - feesPaid, 0)`
- Clients cannot send or edit `feesPaid` or `pendingFees`.
- An **initial payment** entered at registration is saved as a normal `PaymentRecord`, so there is one source of truth.
- Changing the standard fee instantly changes `pendingFees` for every student.
- Known limitation: the formula uses all-time payments against a single monthly fee. True per-month billing would need a different model.

### 6.2 Rooms

- Room number format: `^[A-Z]-[1-9][0-9]{2}$` (e.g. `A-101`), validated on DTO, entity, and service.
- Capacity is fixed at 4 `ACTIVE` residents. Registration or room change into a full room is rejected with HTTP 400.
- Rooms are created automatically when the first resident is registered into them.
- The room row is locked (pessimistic write lock) during registration and moves, so two simultaneous requests cannot take the last bed.
- Room `status` is recalculated after every registration, move, or check-out.

### 6.3 Resident lifecycle

- New residents start as `ACTIVE`.
- `DELETE /api/students/{id}` is a **soft delete (check-out)**: status becomes `CHECKED_OUT`, the bed is freed, and the profile and payments are kept.
- Editable fields: name, admission date, room. Checked-out residents cannot be edited (HTTP 400) but can still receive payments.

## 7. API

All endpoints require an authenticated `ADMIN` session; state-changing calls also need the CSRF header (see Security.md). JSON in, JSON out.

| Method | Path                                                | Body                                                                            | Success                                       |
| ------ | --------------------------------------------------- | ------------------------------------------------------------------------------- | --------------------------------------------- |
| POST   | `/api/students`                                     | `name`, `roomNumber`, `admissionDate`, optional `initialPayment`, `paymentMode` | 201 + student                                 |
| GET    | `/api/students`                                     | none                                                                            | 200 + list (ordered by id)                    |
| GET    | `/api/students/{id}`                                | none                                                                            | 200 + student                                 |
| PUT    | `/api/students/{id}`                                | `name`, `roomNumber`, `admissionDate`                                           | 200 + student                                 |
| DELETE | `/api/students/{id}`                                | none                                                                            | 204 (check-out)                               |
| GET    | `/api/students/reports/pending-fees?minAmount=0.00` | none                                                                            | 200 + students with pending > minAmount       |
| POST   | `/api/payments`                                     | `studentId`, `amount` (>= 0.01), `paymentMode`                                  | 201 + payment                                 |
| GET    | `/api/payments/student/{studentId}`                 | none                                                                            | 200 + payments, newest first                  |
| GET    | `/api/rooms`                                        | none                                                                            | 200 + rooms with occupancy and resident names |
| GET    | `/api/rooms/{roomNumber}`                           | none                                                                            | 200 + room                                    |
| GET    | `/api/settings/standard-monthly-fee`                | none                                                                            | 200 `{name, value}`                           |
| PUT    | `/api/settings/standard-monthly-fee`                | `{value}` (>= 0)                                                                | 200 `{name, value}`                           |

Student response: `id, name, roomNumber, admissionDate, feesPaid, pendingFees, status`.

## 8. Error Handling

`GlobalExceptionHandler` applies only to classes in `com.hostel.controller` (so missing static files still get Spring's normal 404 page instead of a fake 500). Every API error uses one shape:

`{ timestamp, status, error, message, path, fieldErrors: { fieldName: "message" } }`

| Situation                                                                                           | Status |
| --------------------------------------------------------------------------------------------------- | ------ |
| Bean Validation failure (`fieldErrors` filled, keys = DTO field names)                              | 400    |
| Room full, invalid room format, editing a checked-out resident, malformed JSON, bad path/param type | 400    |
| Student or room not found                                                                           | 404    |
| Database constraint conflict                                                                        | 409    |
| Anything unexpected (generic message to client, full stack trace in server log)                     | 500    |

## 9. Guardrails

1. Input is validated server-side regardless of what the browser does.
2. Output is HTML-escaped on the client (see Frontend.md).
3. Credentials and fee seed come only from the environment.
4. No money or occupancy calculation is ever trusted from the client.
