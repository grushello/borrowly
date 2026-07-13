# Borrowly

Spring Boot (Java 21) backend for Borrowly.

## Running locally

### Prerequisites
- Docker Desktop
- JDK 21 (only if you want to run the app or tests outside Docker)

### Everything in Docker
Copy the env template and start the stack:

```bash
cp .env.example .env
docker compose up --build
```

This builds the app image, starts PostgreSQL, waits until the database is
healthy, and then starts the app on http://localhost:8080.

### Database only (for running the app or tests from your IDE)
The tests and a locally run app expect a database on `localhost:5432`. Start
just the database with:

```bash
docker compose up -d db
./mvnw test
```

The `dev` profile (active by default) points at `localhost:5432` with the
credentials from `.env.example`.

## Configuration
Database settings live in Spring profiles:
- `application-dev.properties` — local defaults, used by default.
- `application-prod.properties` — reads `SPRING_DATASOURCE_*` from the
  environment, so no credentials are committed.

The active profile is controlled by `SPRING_PROFILES_ACTIVE` in `.env` (defaults
to `dev`). Inside Docker Compose the datasource URL is set to the `db` service
through environment variables, so the app reaches Postgres by service name
regardless of the profile.

## Deployment (production)

Deployment runs on a **self-hosted GitHub Actions runner** installed on the
target server. A workflow builds and starts the stack there with
`docker compose`, so "the runner" and "the host" are the same machine.

Production secrets are **not** stored in the repo. They live in the repository's
GitHub Actions secrets (Settings → Secrets and variables → Actions), and the
deploy workflow writes them into a `.env` file on the server at deploy time.

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
