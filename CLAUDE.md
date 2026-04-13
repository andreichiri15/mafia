# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mafia is a social deduction game with a Spring Boot 3.5 (Java 21) backend and a React + Vite + TypeScript frontend. PostgreSQL 16 is the database, run via Docker Compose.

## Build & Run Commands

### Database
```bash
docker compose up -d          # Start PostgreSQL (port 5433)
```

### Backend (from `backend/`)
```bash
mvn spring-boot:run           # Run dev server (hot reload via devtools)
mvn clean package             # Build JAR
mvn test                      # Run all tests
mvn -Dtest=ClassName test     # Run a single test class
mvn -Dtest=ClassName#method test  # Run a single test method
```

### Frontend (from `web-frontend/`)
```bash
npm i                         # Install dependencies
npm run dev                   # Dev server on port 3000
npm run build                 # Production build to build/
```

## Architecture

### Backend
- **Package root:** `com.andreichiri.mafia_backend`
- **Entities:** `entity/` — MafiaUser, Lobby, LobbyPlayer, Game, GamePlayer, GameAction, Message
- **Controllers:** `controller/` — AuthController (`/api/auth/**`), LobbyController (`/api/lobbies`), Controller (test endpoint)
- **Services:** `service/` — AuthService, LobbyService, JwtTokenProvider
- **Security:** JWT-based auth. `JwtAuthenticationFilter` extracts Bearer tokens, `JwtTokenProvider` handles creation/validation (HMAC-SHA256 via JJWT). Auth endpoints are public; all others require authentication. Config in `SecurityConfig`.
- **DTOs:** `dto/` — LobbyDTO (request/response records), LobbySummaryReport
- **Repositories:** Spring Data JPA repositories in `repositories/`

### Frontend
- **Router:** `App.tsx` — routes: `/`, `/play`, `/lobby`, `/profile`, `/signin`
- **Pages:** `components/` — HomePage, PlayPage, LobbyPage, ProfilePage, SignInPage, Navigation
- **UI library:** shadcn/ui components in `components/ui/` (Radix primitives + Tailwind CSS v4)
- **State:** Zustand store in `store/authStore.tsx`
- **Path alias:** `@` maps to `./src`

### Database
- Hibernate JPA with `ddl-auto=update` (schema auto-managed)
- Connection: `localhost:5433/mafia`, user `mafia`

## Key Patterns
- Backend DTOs use Java records (e.g., `CreateLobbyRequest`, `LobbyDetailResponse`)
- `UserPrincipal` is a record used as the authentication principal in Spring Security context
- Frontend uses shadcn/ui conventions: components in `ui/`, composed with `cn()` utility from `clsx` + `tailwind-merge`
- Game phases: NIGHT → DAY → VOTING → GAME_OVER; roles: MAFIA, VILLAGER, SHERIFF
