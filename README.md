# RepairSystem

RepairSystem is the git repository that now contains the Nuxt frontend and the
RepairAuto Spring Boot backend.

## Project Layout

- `repairSystem/` - existing Nuxt frontend, left unchanged by backend work
- `backend/` - RepairAuto backend, currently Phase 2 core reference data

No frontend source was changed as part of the backend integration. The backend
is available for the frontend team to wire when they are ready.

## Run The Backend Stack

From this repository root:

```bash
docker compose up --build
```

Default local URLs:

- Backend health: `http://localhost:8080/actuator/health`

The root compose file creates a development admin account by default:

- Email: `admin@example.com`
- Password: `ChangeMe123!`

Override secrets and bootstrap values with environment variables before using
anything beyond local development.

## Verify

Backend:

```bash
cd backend
./gradlew clean check bootJar
```
