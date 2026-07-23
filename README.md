# Borrowly

Spring Boot (Java 21) backend for Borrowly.

## Quick start

```bash
cp .env.example .env
docker compose up --build
```

Starts PostgreSQL and the app on http://localhost:8080. The only prerequisite is
Docker.

To run the tests from your IDE, start just the database first:

```bash
docker compose up -d db
./mvnw test
```

### Test admin account

Signup always assigns the `USER` role, so the first `ADMIN` cannot be created
through the app. Changeset `014-seed-test-admin` seeds one on a fresh database:

| Email | Password |
| --- | --- |
| `admin@borrowly.local` | `Admin123!` |

That account unlocks the admin panel, category management, and user
enable/disable. The changeset is guarded by a precondition, so it is skipped if
the email already exists.

## Documentation

- [infrastructure.md](docs/infrastructure.md) — Docker, PostgreSQL,
  Liquibase migrations, configuration/profiles, and deployment.
- [work_conventions.md](docs/work_conventions.md) — branching, commits, and PR rules.
