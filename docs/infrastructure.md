# Infrastructure: Docker, PostgreSQL, Liquibase

This document describes how Borrowly is containerized, how it connects to
PostgreSQL, how database migrations work, and how the same setup moves to
production. It covers the `build/docker-postgres-liquibase` work.

## Overview

The app runs as two containers managed by Docker Compose:

- **app** — the Spring Boot service, built from the `Dockerfile`.
- **db** — a PostgreSQL database.

The only thing a machine needs to run all of this is Docker. Java and Maven are
used *inside* the build, not on the host.

```
docker compose
├── app   (Spring Boot, port 8080)  ──connects to──▶  db:5432
└── db    (PostgreSQL, port 5432, data in a named volume)
```

## Docker

### Dockerfile (multi-stage)

The image is built in two stages so the final image stays small.

1. **build stage** (`maven:3.9-eclipse-temurin-21`) — has Maven + JDK 21. It
   copies `pom.xml` first and resolves dependencies on their own layer (cached
   until `pom.xml` changes), then copies the source and runs
   `mvn clean package -DskipTests`.
2. **runtime stage** (`eclipse-temurin:21-jre-jammy`) — only a Java runtime. It
   creates a non-root user, copies the jar from the build stage, and runs it.

Tests are skipped **in the image build** on purpose: the build's job is to
produce a runnable jar, and the tests spin up their own database through Docker
(not available mid-build). They run locally and in CI instead (see
[Tests](#tests)).

The final image carries only the JRE + the jar (no Maven, no source), and runs
as a non-root user.

### .dockerignore

Keeps the build context small and avoids copying junk or secrets into the image:
`target/`, `.git/`, `.idea/`, `*.md`, `.mvn/`, `mvnw*`, `.env`.

## Configuration and profiles

Database settings live in Spring profiles:

- `application.properties` — shared settings, plus `spring.profiles.active=dev`
  as the default for runs outside Docker (IDE, tests). Tests keep this profile
  but override the datasource to a throwaway container (see [Tests](#tests)).
- `application-dev.properties` — local defaults pointing at `localhost:5432`
  with local credentials. Safe to commit (a dev-only database).
- `application-prod.properties` — every datasource value comes from an
  environment variable (`${SPRING_DATASOURCE_URL}`, etc.). No credentials in the
  repo. If a required variable is missing, the app fails fast at startup.

### The dev ↔ prod switch

The active profile is controlled by `SPRING_PROFILES_ACTIVE` in `.env`. Set it to
`dev` locally or `prod` on the server. That one variable is the whole switch —
same image, same compose file.

### How `.env` names reach the app

The app expects `SPRING_DATASOURCE_*` variables, but `.env` defines
`POSTGRES_*`. They are different on purpose: the Postgres image only reads
`POSTGRES_*`, and Spring only reads `SPRING_DATASOURCE_*`. Compose bridges them
in the `app` service's `environment:` block:

```
.env                     docker-compose.yml                container sees
POSTGRES_USER=borrowly ─▶ SPRING_DATASOURCE_USERNAME:    ─▶ SPRING_DATASOURCE_
                          ${POSTGRES_USER}                   USERNAME=borrowly
```

Because environment variables outrank profile files in Spring, inside Compose
the datasource URL is always `db:5432` (the database service name) regardless of
which profile is active.

## PostgreSQL (Docker Compose)

The `db` service:

- Image `postgres:16-alpine`, credentials from `.env`.
- A **named volume** (`postgres_data`) stores the data, so it survives
  `docker compose down`.
- A **healthcheck** (`pg_isready`) reports when Postgres is ready to accept
  connections.

The `app` service uses `depends_on: condition: service_healthy`, so it waits for
the database's healthcheck to pass before starting — avoiding "app started before
the DB was ready" crashes.

### Secrets

- `.env` holds real values and is **gitignored**.
- `.env.example` is committed as a template; copy it with `cp .env.example .env`.

## Database migrations (Liquibase)

Liquibase owns the database schema. Hibernate is set to only validate it
(`spring.jpa.hibernate.ddl-auto=validate`) — it checks that the tables match the
entities and never changes the schema itself. If an entity is added without a
matching migration, the app fails to start, which forces migrations to be
written.

On startup Spring Boot runs Liquibase automatically (the `liquibase-core`
dependency plus a datasource). It reads the master changelog:

```
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
```

Liquibase tracks what it has already applied in two tables it creates itself:
`databasechangelog` and `databasechangeloglock`.

### Adding a migration

1. Create a changeset file under `src/main/resources/db/changelog/changes/`,
   named with an incrementing prefix, e.g. `001-create-users.yaml`.
2. Include it in `db.changelog-master.yaml`:

   ```yaml
   databaseChangeLog:
     - include:
         file: db/changelog/changes/001-create-users.yaml
   ```

3. Write the change, for example:

   ```yaml
   databaseChangeLog:
     - changeSet:
         id: 001-create-users
         author: your-name
         changes:
           - createTable:
               tableName: users
               columns:
                 - column:
                     name: id
                     type: bigint
                     autoIncrement: true
                     constraints:
                       primaryKey: true
   ```

**Rule:** never edit a changeset after it has run on a shared database. Add a new
changeset instead — Liquibase decides what to apply by comparing IDs against the
tracking table, so editing an old one leaves environments out of sync.

## Running locally

### Prerequisites
- Docker Desktop
- JDK 21 (only if you run the app or tests outside Docker)

### Everything in Docker

```bash
cp .env.example .env
docker compose up --build
```

Builds the app image, starts PostgreSQL, waits until it is healthy, then starts
the app on http://localhost:8080.

### Tests

Tests need no database started by hand. Any test that boots a Spring context
(the `@SpringBootTest` and `@DataJpaTest` classes) spins up its own disposable
`postgres:16-alpine` container via Testcontainers, runs the Liquibase changesets
into it, and discards it when the run ends:

```bash
./mvnw test
```

The only requirement is a running Docker daemon — Testcontainers manages the
container. It matches the production Postgres version, so the same
Postgres-specific changesets run in tests as in prod. Pure unit tests (models,
DTOs, mappers) don't touch a database at all.

### Database only (for running the app from your IDE)

A locally run app expects a database on `localhost:5432`:

```bash
docker compose up -d db
./mvnw spring-boot:run
```

### Development workflow: rebuilds vs. hot reload

`docker compose up --build` is a one-shot: it rebuilds the image from the current
source and then starts it. It is **not** hot reload. Once the container is
running it holds a fixed jar, and editing a source file on your machine does not
change it — the source was compiled into the jar at build time and nothing on the
host is mounted into the container. To pick up a code change you run `--build`
again.

For day-to-day development, don't run the app in Docker. Run only the database in
Docker and the app on your host, so the IDE compiles on save:

```bash
docker compose up -d db      # Postgres in a container
./mvnw spring-boot:run       # app on your machine (dev profile -> localhost:5432)
```


Use the full `docker compose up --build` to verify the production-like container
setup — for example before opening a pull request — not as your editing loop.

## Deployment (production)

Deployment runs on a **self-hosted GitHub Actions runner** installed on the
target server, so "the runner" and "the host" are the same machine. A workflow
builds and starts the stack there with `docker compose`.

Production secrets are **not** stored in the repo. They live in the repository's
GitHub Actions secrets (Settings → Secrets and variables → Actions), and the
deploy workflow writes them into a `.env` on the server at deploy time.

Required secrets: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.

Example `.github/workflows/deploy.yml` (not added yet):

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: self-hosted
    steps:
      - uses: actions/checkout@v4

      - name: Write .env from secrets
        run: |
          cat > .env <<EOF
          SPRING_PROFILES_ACTIVE=prod
          POSTGRES_DB=${{ secrets.POSTGRES_DB }}
          POSTGRES_USER=${{ secrets.POSTGRES_USER }}
          POSTGRES_PASSWORD=${{ secrets.POSTGRES_PASSWORD }}
          EOF

      - name: Build and start
        run: docker compose up -d --build
```

With `SPRING_PROFILES_ACTIVE=prod` the app loads `application-prod.properties`
and reads its database settings from the environment. The generated `.env` stays
on the server and is never committed.

## Not done yet / future work

- The `deploy.yml` above is a reference, not a committed workflow. The real one
  needs the lecturers' server details (working directory, port/reverse proxy).
- `docker-compose.prod.yml` override (no published DB port, resource limits).
- Silence the `spring.jpa.open-in-view` warning by setting it to `false`.
- `mvnw` is missing its executable bit (`git update-index --chmod=+x mvnw`).
