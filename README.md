# ETRA-CTI

ETRA-CTI is an application security and cyber-threat intelligence platform with:

- A Spring Boot backend
- A React/Vite frontend
- A PostgreSQL database

## Prerequisites

Install or have access to:

- Java 21
- Node.js and npm
- Docker and Docker Compose

The backend includes a Maven wrapper, so Maven does not need to be installed globally.

## Run the application

Run the following commands in three separate terminals.

### 1. Start PostgreSQL

```bash
cd /home/natty/aegis-project/ETRA-CTI
docker compose up -d postgres
```

Check the database status:

```bash
docker compose ps
```

The database uses:

```text
Host:     127.0.0.1
Port:     55432
Database: etra_cti
Username: etra
Password: etra_secret
```

### 2. Start the backend

```bash
cd /home/natty/aegis-project/ETRA-CTI/backend
chmod +x mvnw
export JWT_SECRET=$(openssl rand -base64 48)
./mvnw spring-boot:run
```

The backend runs on:

```text
http://localhost:8081
```

Verify the backend:

```bash
curl http://localhost:8081/actuator/health
```

The response should contain:

```json
{
  "status": "UP"
}
```

Flyway applies the database migrations automatically during backend startup.

### 3. Start the frontend

```bash
cd /home/natty/aegis-project/ETRA-CTI/frontend
npm ci
npm run dev
```

The frontend runs on:

```text
http://localhost:3000
```

Open that URL in a browser.

The Vite development server forwards `/api` and `/actuator` requests to the backend on port `8081`.

## Create a local user

The login screen displays these demo credentials:

```text
Email:    admin@etra.local
Password: Password123!
```

The project does not automatically seed this user. If the user does not exist, create it:

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@etra.local",
    "password": "Password123!",
    "fullName": "System Administrator"
  }'
```

New users are assigned the `VIEWER` role. For local development, promote the account to administrator:

```bash
docker exec -it etra-postgres psql \
  -U etra \
  -d etra_cti \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'admin@etra.local';"
```

Log out and sign in again after changing the role so that a new JWT is issued.

## Check existing users

```bash
docker exec -it etra-postgres psql \
  -U etra \
  -d etra_cti \
  -c "SELECT email, full_name, role, enabled FROM users;"
```

## Stop the application

Stop the frontend and backend with `Ctrl+C` in their terminals.

Stop PostgreSQL with:

```bash
cd /home/natty/aegis-project/ETRA-CTI
docker compose down
```

To remove the database volume as well, which deletes local database data:

```bash
docker compose down -v
```

## Service URLs

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8081 |
| Backend health | http://localhost:8081/actuator/health |
| PostgreSQL | 127.0.0.1:55432 |
