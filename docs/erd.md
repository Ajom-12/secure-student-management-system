# Student Management System ERD

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : owns
    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password "BCrypt hash"
        varchar role "ADMIN, TEACHER, STUDENT"
    }
    STUDENTS {
        bigint id PK
        varchar registration_number UK
        varchar full_name
        varchar email UK
        varchar phone
        varchar gender
        date date_of_birth
    }
    REFRESH_TOKENS {
        bigint id PK
        varchar token_hash UK
        bigint user_id FK
        timestamptz expiry_date
        boolean revoked
    }
```

`users.role` stores one enum role on each account. Refresh tokens belong to a
user and are stored as SHA-256 hashes. A student record is independent of a
login account; connect these concepts with a foreign key only if the product
later requires every student to sign in.
