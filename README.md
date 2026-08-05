# RepairSystem

RepairSystem is the git repository that contains the Nuxt frontend and the
RepairAuto Spring Boot backend.

## Project Layout

- `repairSystem/` - Nuxt frontend
- `backend/` - RepairAuto backend, currently Phase 2 core reference data

## Run The Backend Stack

From this repository root:

```bash
docker compose up --build
```

Default local URLs:

- Backend health: `http://localhost:8080/actuator/health`
- Frontend: `http://localhost:3000`

The root compose file creates a development admin account by default:

- Email: `admin@example.com`
- Password: `ChangeMe123!`

Override secrets and bootstrap values with environment variables before using
anything beyond local development.

## Deploy On Render

This repo includes a Render Blueprint at `render.yaml` for the Nuxt frontend,
Spring Boot backend, and Render Postgres database.

Create a new Blueprint instance in Render from this repository. During the first
sync, Render prompts for `APP_BOOTSTRAP_ADMIN_EMAIL` and
`APP_BOOTSTRAP_ADMIN_PASSWORD`. Use a real admin email and a strong temporary
password, sign in once after deploy, then set `APP_BOOTSTRAP_ADMIN_ENABLED=false`
on the backend service.

The default service URLs in `render.yaml` are:

- Frontend: `https://repair-system-frontend.onrender.com`
- Backend: `https://repair-system-backend.onrender.com`

If Render assigns different hostnames or you add custom domains, update
`NUXT_BACKEND_URL` on the frontend and `APP_CORS_ALLOWED_ORIGINS` on the backend.
Object storage and Telegram are disabled by default for Render; enable them only
after adding the required provider credentials.

## Verify

Backend:

```bash
cd backend
./gradlew clean check bootJar
```
