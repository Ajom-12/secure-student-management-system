# Student Management System

A REST API built with Spring Boot, Spring Security, Spring Data JPA, and
PostgreSQL. The application separates HTTP controllers, services, repositories,
and persistence entities. It supports registration, login, JWT-protected
student CRUD, administrative user/role management, refresh-token rotation, and
logout revocation.

## Technologies and prerequisites

- Java 21
- PostgreSQL 14 or newer
- Maven (or the included Maven Wrapper)
- Postman (optional, for the supplied API collection)

## Configure and run

Create a database, for example:

```sql
CREATE DATABASE student_db;
```

Set the following environment variables to override the local-development
defaults in `src/main/resources/application.properties`:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/student_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | PostgreSQL user |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `JWT_SECRET` | local-only development key | Base64-encoded key; use a randomly generated secret of at least 32 bytes outside local development |
| `JWT_ACCESS_TOKEN_TTL_MS` | `900000` | Access-token lifetime (15 minutes) |
| `JWT_REFRESH_TOKEN_TTL_MS` | `604800000` | Refresh-token lifetime (7 days) |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:3000` | Single allowed frontend origin |
| `BOOTSTRAP_ADMIN_USERNAME` | empty | Optional first ADMIN account username |
| `BOOTSTRAP_ADMIN_EMAIL` | empty | Optional first ADMIN account email |
| `BOOTSTRAP_ADMIN_PASSWORD` | empty | Optional first ADMIN account password |

The three bootstrap-admin variables must either all be blank or all be set.
Configure them before the first start to create an ADMIN; the starter does not
silently grant elevated access through public registration. Public registration
always creates a `STUDENT`. Change all development secrets/passwords before
deploying.

The SQL migration is at
[`db/migration/V1__create_student_management_schema.sql`](db/migration/V1__create_student_management_schema.sql).
Run it with your preferred migration runner, or start the application with the
provided Hibernate `ddl-auto=update` development setting to create/update the
tables automatically. Production deployments should use versioned migrations
and disable automatic schema updates.

From the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

The API listens at `http://localhost:8080`. Build and run tests with:

```powershell
.\mvnw.cmd test
```

Automated tests use an isolated in-memory H2 database; the running application
uses PostgreSQL. Apply the migration (or allow Hibernate schema update) before
starting the application against a clean PostgreSQL database.

## Endpoints

| Method | Path | Authentication / role |
|---|---|---|
| POST | `/api/auth/register` | Public; always registers STUDENT |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public; requires a valid refresh token in the body |
| POST | `/api/auth/logout` | Public; revokes the refresh token in the body |
| GET | `/api/students` | Any authenticated role |
| GET | `/api/students/{id}` | Any authenticated role |
| POST | `/api/students` | ADMIN or TEACHER |
| PUT | `/api/students/{id}` | ADMIN or TEACHER |
| DELETE | `/api/students/{id}` | ADMIN |
| GET | `/api/users` | ADMIN |
| PUT | `/api/users/{id}/role` | ADMIN |

Student fields are `registrationNumber`, `fullName`, `email`, `phone`, `gender`,
and `dateOfBirth` (`YYYY-MM-DD`). Registration requires a 3-50 character
username, valid email, and 12-72 character password. Passwords use BCrypt.
Student registration number and email, and user username and email, are unique.

## Authentication, roles, and tokens

Registration establishes an account; login verifies credentials and returns
`accessToken`, `refreshToken`, `tokenType`, `expiresIn`, and the account role.
Send **only the access token** to protected API routes:

```http
Authorization: Bearer <accessToken>
```

Authentication establishes identity. Authorization then checks that identity's
role against the operation. Missing/invalid access authentication returns 401;
an authenticated but disallowed role receives 403.

JWT (JSON Web Token) is a signed, time-limited access credential. For each
protected request the security filter extracts and verifies the signature and
expiry, reloads the user and authorities, and establishes the Spring Security
authentication before controller authorization. Access tokens are stateless
and expire after 15 minutes by default.

Refresh tokens are random opaque values, separate from JWT access tokens. The
database stores only their SHA-256 hashes. `/api/auth/refresh` consumes the
presented token and returns a new access/refresh pair (rotation); a used,
revoked, unknown, or expired token is rejected. `/api/auth/logout` revokes the
provided refresh token, preventing future refresh. Logout does not immediately
invalidate an already-issued stateless access token; it remains usable until
its short expiry. Clients should discard both tokens at logout.

Roles are `ADMIN`, `TEACHER`, and `STUDENT`. Registration defaults to STUDENT.
Set the optional bootstrap ADMIN environment variables to create the initial
administrator, then use `PUT /api/users/{id}/role` to promote accounts to
TEACHER or other roles.

## CORS and CSRF

CORS allows the configured single frontend origin, `http://localhost:3000` by
default, and only the API methods/headers needed by this application. Set
`CORS_ALLOWED_ORIGIN` for another frontend. CORS controls which browser origins
may read API responses; it is not authentication.

CSRF protection is disabled intentionally because this API is stateless and
accepts bearer tokens explicitly in the `Authorization` header; it does not
authenticate requests using automatically attached cookies or server sessions.
Do not store tokens in cookies without adding CSRF defenses and revisiting this
configuration.

## Postman demonstration

Import [`postman/Student-Management-System.postman_collection.json`](postman/Student-Management-System.postman_collection.json)
and optionally set collection variables. The collection runs assertions and
stores response tokens/IDs where possible.

1. Start the service and create a bootstrap ADMIN using environment variables.
2. Register a STUDENT with the collection's Register request; duplicate
   registration is included as a separate expected-conflict request.
3. Login as student and ADMIN; to demonstrate TEACHER operations, promote a
   registered account using `PUT /api/users/{id}/role` as ADMIN, then log in as
   that account.
4. Run the protected requests to verify missing-token 401, role-based 403,
   teacher create/update, admin delete, validation, not-found, refresh rotation,
   and logout/replay rejection.
5. The CORS preflight request checks the configured development origin and
   response headers. Browsers enforce CORS; Postman itself does not enforce it.

Postman captures actual request/response examples in its run results when the
collection is run against a configured local instance. No execution screenshots
are pre-generated because they depend on local PostgreSQL credentials and the
running service; export the Postman Runner results or capture those results
after completing the sequence.

## Deliverables and explanation

- Database setup: [`db/migration/V1__create_student_management_schema.sql`](db/migration/V1__create_student_management_schema.sql)
- Main relationships: [`docs/erd.md`](docs/erd.md)
- API demonstrations: [`postman/Student-Management-System.postman_collection.json`](postman/Student-Management-System.postman_collection.json)

Request path: client -> CORS/security filter chain -> JWT authentication filter
-> role authorization -> controller -> service -> Spring Data repository ->
PostgreSQL. The global exception handler formats validation, duplicate,
not-found, and invalid-credential errors.
