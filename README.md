# Hostel Management System

A single-admin web dashboard for a hostel owner or warden. It handles resident registration, room allocation (max 4 per room), payment tracking, and automatic pending-fee calculation.

## Features

- Register, edit and check out residents (check-out is a soft delete; history is kept)
- Rooms are created automatically, capped at 4 active residents each, with a live room overview
- Record payments (Cash / UPI / Card / Bank transfer) and view each resident's payment history
- Pending fees are calculated on the server: standard monthly fee minus all payments
- Change the standard monthly fee from the dashboard at any time
- Search residents and filter by pending dues
- Secure login (BCrypt, CSRF protection, session-based)

## Tech Stack

Java 21, Spring Boot 3.4.3 (Web, Data JPA, Validation, Security), H2 file database, vanilla JavaScript, Tailwind CSS (CDN).

## Prerequisites

- JDK 21
- Git

Maven is **not** required; the included wrapper (`mvnw`) downloads it.

## Setup

1. Clone the repository and open the project folder.
2. Create a `.env` file next to `pom.xml` (copy `.env.example`):

```
   HOSTEL_ADMIN_USERNAME=admin
   HOSTEL_ADMIN_PASSWORD=choose-a-strong-password
   HOSTEL_STANDARD_MONTHLY_FEE=5000.00
```

The fee value is only a first-run seed. After the first start, change it from the dashboard.

3. Run the app:

```
   # Windows
   mvnw.cmd spring-boot:run

   # macOS / Linux
   ./mvnw spring-boot:run
```

4. Open http://localhost:8080 and log in with the username and password from `.env`.

## Usage

| Task                    | How                                                        |
| ----------------------- | ---------------------------------------------------------- |
| Add a resident          | Fill the form on the left. An initial payment is optional. |
| Record a later payment  | Click the rupee icon on the resident's row.                |
| Edit or move a resident | Click the pencil icon (active residents only).             |
| Check out a resident    | Click the exit icon and confirm. The bed is freed.         |
| See room occupancy      | Sidebar, then Rooms.                                       |
| Change the monthly fee  | Sidebar, then Monthly fee.                                 |

Room numbers use the format `A-101` (one letter, a hyphen, then three digits).

## Project Structure

```
src/main/java/com/hostel/
  config/        Security configuration
  controller/    REST controllers + global error handler
  service/       Business rules (fees, rooms, payments)
  repository/    Spring Data JPA repositories
  entity/        Student, Room, PaymentRecord, SystemSetting
  dto/           Request/response records
  exception/     Custom exceptions
src/main/resources/
  application.properties
  static/        index.html, app.js, error pages
docs/            System.md, Security.md, Frontend.md
```

## API Overview

All endpoints need a logged-in admin session.

| Area      | Endpoints                                                                                               |
| --------- | ------------------------------------------------------------------------------------------------------- |
| Residents | `GET/POST /api/students`, `GET/PUT/DELETE /api/students/{id}`, `GET /api/students/reports/pending-fees` |
| Payments  | `POST /api/payments`, `GET /api/payments/student/{id}`                                                  |
| Rooms     | `GET /api/rooms`, `GET /api/rooms/{roomNumber}`                                                         |
| Settings  | `GET/PUT /api/settings/standard-monthly-fee`                                                            |

Full details are in [docs/System.md](docs/System.md).

## Documentation

- [System specification](docs/System.md)
- [Security specification](docs/Security.md)
- [Frontend specification](docs/Frontend.md)

## Database and H2 Console

Data is stored in `./data/hosteldb` (created on first run, not committed). The H2 console is at `/h2-console` (admin login required). Use JDBC URL `jdbc:h2:file:./data/hosteldb;AUTO_SERVER=TRUE`, user `sa`, empty password.

To reset all data, stop the app and delete the `data/` folder.

## Troubleshooting

| Problem                                                 | Fix                                                                                                 |
| ------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `Could not resolve placeholder 'HOSTEL_ADMIN_USERNAME'` | `.env` is missing or misnamed. It must be exactly `.env` (not `.env.txt`) and sit beside `pom.xml`. |
| Errors after pulling schema changes                     | Stop the app, delete `data/`, start again.                                                          |
| Page looks unstyled                                     | Tailwind and FontAwesome load from a CDN, so check your internet connection.                        |
| 403 when saving                                         | Refresh the page (stale security token).                                                            |

## Security Notes

- Never commit `.env` or `data/`.
- This setup is for local or internal use. Before putting it on the internet, follow the production checklist in [docs/Security.md](docs/Security.md).
