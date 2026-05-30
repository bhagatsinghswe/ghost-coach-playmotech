# 👻 Ghost Coach — AI-Powered Sports Coaching Assistant

> Built for Playmotech Ghost Coach Assignment · Role: Backend Engineer (Full Stack submission — Spring Boot + React 18)

---

## 🚀 Quick Start (under 3 minutes — backend only)

> **Reviewer note:** No database setup, no paid API key, no Docker needed.
> Just Java + Maven + a free Gemini key.

### Prerequisites
- Java 17+
- Maven 3.8+
- Free Gemini API key → [aistudio.google.com](https://aistudio.google.com) (30 seconds to get one)

### 1. Clone & configure

```bash
git clone <your-repo-url>
cd ghost-coach
cp .env.example .env
# Edit .env — set GEMINI_API_KEY and JWT_SECRET
```

### 2. Run the backend

```bash
cd backend
# Export env vars (or set in your IDE)
export GEMINI_API_KEY=your-key-here
export JWT_SECRET=any-64-char-string-here

mvn spring-boot:run
# → http://localhost:8080
# → H2 Console: http://localhost:8080/h2-console
```

### 3. Run the frontend

```bash
cd frontend
npm install
# Windows (PowerShell):
$env:REACT_APP_API_URL="http://localhost:8080"; npm start

# Mac/Linux:
REACT_APP_API_URL=http://localhost:8080 npm start

# → http://localhost:3000
```

### Docker (one command)

```bash
cp .env.example .env   # fill in GEMINI_API_KEY and JWT_SECRET
docker-compose up --build
# Backend: http://localhost:8080
# Frontend: http://localhost:3000
```

---

## Features Built

| # | Feature | Status |
|---|---------|--------|
| 1 | Player Registration & Authentication (JWT) | ✅ Complete |
| 2 | Stance Upload & AI Coaching Feedback (Gemini Vision) | ✅ Complete |
| 3 | Session History with pagination | ✅ Complete |
| 4 | AI Improvement Chat (context-aware) | ✅ Complete |
| B1 | Score progress chart across sessions | ✅ Complete (bonus) |
| B2 | Mobile-responsive UI | ✅ Complete (bonus) |

| F1 | React UI — Register, Login, Upload, History, Chat, Profile | ✅ Complete |
| F2 | Drag-and-drop upload with live AI polling | ✅ Complete |
| F3 | Score progress chart (Recharts) | ✅ Complete |

---

## Architecture

```
ghost-coach/
├── backend/                    # Spring Boot 3 (Java 17)
│   └── src/main/java/com/playmotech/ghostcoach/
│       ├── config/             # Security, WebClient, AppProperties
│       ├── controller/         # AuthController, SessionController,
│       │                       # ChatController, ImageController
│       ├── dto/                # Request/Response DTOs
│       │   ├── request/        # RegisterRequest, LoginRequest, ChatMessageRequest
│       │   └── response/       # ApiResponse, AuthResponse, SessionResponse,
│       │                       # CoachingFeedback, ChatMessageResponse, ...
│       ├── entity/             # User, CoachingSession, ChatMessage
│       ├── exception/          # GlobalExceptionHandler + typed exceptions
│       ├── repository/         # Spring Data JPA repositories
│       ├── security/           # JwtUtils, JwtAuthenticationFilter,
│       │                       # UserDetailsServiceImpl
│       ├── service/            # AuthService, CoachingSessionService,
│       │                       # ChatService, GeminiVisionService
│       └── util/               # FileStorageUtil, SessionMapper
└── frontend/                   # React 18
    └── src/
        ├── components/layout/  # Navbar
        ├── hooks/              # useAuth (AuthContext)
        ├── pages/              # Register, Login, Upload, History,
        │                       # SessionDetail
        ├── services/           # api.js (Axios + JWT interceptor)
        └── styles/             # global.css
```

### Stack choices

| Layer | Choice | Reasoning |
|-------|--------|-----------|
| Backend | Spring Boot 3 + Java 17 | Required by JD; battle-tested REST + DI |
| Database (dev) | H2 file DB | Zero-config local dev; schema auto-creates |
| Database (prod) | PostgreSQL | Relational fits the session/user/chat model well |
| AI API | Gemini 1.5 Flash | Free, vision-capable, fast; no billing setup |
| Auth | JWT (HS256, 24h expiry) | Stateless; no session store needed |
| Image storage | Local filesystem | Simple for assignment scope; S3 path easy to add |
| HTTP client | WebFlux WebClient | Non-blocking Gemini calls; keeps upload endpoint snappy |
| Async | Spring @Async | Upload returns instantly; AI runs in background thread |

---

## API Reference

All endpoints return `{ success, data, message }`.

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | — | Create account, returns JWT |
| POST | `/api/auth/login` | — | Returns JWT |

### Sessions
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/sessions/upload` | ✅ | Upload image, trigger AI analysis |
| GET | `/api/sessions` | ✅ | Paginated history (`?page=0&size=10`) |
| GET | `/api/sessions/{id}` | ✅ | Full session + feedback |
| GET | `/api/sessions/progress` | ✅ | Score-over-time data points |

### Chat
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/sessions/{id}/chat` | ✅ | Send message, get AI reply |
| GET | `/api/sessions/{id}/chat` | ✅ | Chat history for session |

## 🧪 Test in 60 Seconds (copy-paste ready)

### 1. Register
POST http://localhost:8080/api/auth/register
Content-Type: application/json
{
"fullName": "Test Player",
"email": "test@test.com",
"password": "Test1234!",
"sport": "CRICKET",
"position": "Batsman",
"experienceLevel": "BEGINNER"
}

### 2. Login — copy the `token` from response
POST http://localhost:8080/api/auth/login
Content-Type: application/json
{"email":"test@test.com","password":"Test1234!"}

### 3. Upload a stance photo (add `Authorization: Bearer <token>`)
POST http://localhost:8080/api/sessions/upload
multipart/form-data → key: file → any .jpg image

### 4. Get AI feedback (wait ~15s after upload)
GET http://localhost:8080/api/sessions/1
Authorization: Bearer <token>

---

## AI Prompt Design

The core insight: **the prompt is the product**. A generic "analyse this sports image" 
yields generic feedback. Ghost Coach injects four layers of player context before any image analysis:

```
1. PERSONA     Ghost Coach as a sport-specialist (not a generic AI)
2. PLAYER PROFILE  sport, position, level, age → personalised depth & tone
3. SPORT CUES  domain-specific features to look for (grip, stance, elbow, etc.)
4. OUTPUT SCHEMA   strict JSON rubric to make parsing deterministic
```

**Level-aware tone**: A `BEGINNER` gets "keep your eyes on the ball, not the bowler" while an 
`ADVANCED` player receives biomechanical specifics. This is injected as an explicit tone instruction.

**Sport-specific visual cues** guide the model's attention:
- Cricket → grip, backlift, head position, weight transfer
- Football → foot placement, knee bend, body orientation
- Basketball → BEEF (Balance, Eyes, Elbow, Follow-through)
- Badminton → grip type, wrist angle, ready position

**Temperature 0.3 for analysis** (consistent, reliable) vs **0.7 for chat** (conversational).

**Markdown stripping**: Gemini sometimes wraps JSON in ` ```json ``` ` despite instructions.
The parser strips these fences before parsing.

---

## Database Schema

```
users
  id, full_name, email, password (bcrypt), sport, position,
  experience_level, age, created_at

coaching_sessions
  id, user_id (FK), original_file_name, storage_path,
  content_type, file_size_bytes,
  overall_score, strengths_json, areas_to_improve_json,
  priority_fix, drill_suggestion, confidence_level,
  raw_ai_response, status (PENDING|PROCESSING|COMPLETED|FAILED),
  error_message, created_at

chat_messages
  id, session_id (FK), role (USER|ASSISTANT), content, created_at
```

Indexes on `sessions.user_id`, `sessions.created_at`, `chat_messages.session_id` 
keep history queries fast even as the dataset grows.

---

## 💡 Key Design Decisions

**Why async AI analysis?**
The upload endpoint returns immediately with a `PENDING` session so the user
never waits on a network request. Gemini runs in a background thread and updates
the session status to `COMPLETED` or `FAILED`. This scales cleanly to a queue
(SQS/RabbitMQ) with zero interface change.

**Why store raw AI response?**
`raw_ai_response` is persisted on every session. If the parsing logic changes
or a bug is found, feedback can be re-parsed from stored data without re-calling
the API.

**Why H2 for dev?**
Zero friction for reviewers. The schema auto-creates on first run. Switching to
PostgreSQL is a single `application-prod.properties` profile change — no code
changes needed.

**Why Gemini 1.5 Flash over GPT-4V?**
Free tier with no billing setup. You (the reviewer) can test the full AI flow
without entering a credit card.

## Known Limitations & What I'd Fix With More Time

| Limitation | Fix |
|-----------|-----|
| Images stored on local disk | Swap `FileStorageUtil.store()` for S3 with a one-line change |
| No email verification | Add Spring Mail + token table |
| AI analysis is fire-and-forget (player must refresh) | Add WebSocket or SSE to push completion to frontend |
| No rate limiting on upload | Add Bucket4j token-bucket per user |
| Chat history grows unbounded | Trim context window to last N messages before Gemini call |
| No refresh tokens	Implement a refresh token endpoint with rotation
No Swagger/OpenAPI docs	Add springdoc-openapi; skipped to keep scope tight
Frontend is basic	Assignment focus was backend; React UI is functional but minimal

---

## What I'd Build Next (Real Product)

1. **Video analysis** — accept short clips, extract frames at key moments (release point, contact, landing), analyse the sequence rather than a single frame.
2. **Coach dashboard** — human coaches can review AI feedback, annotate images, and override scores.
3. **Team management** — academy/club accounts with player groups.
4. **Drill library** — surface past drill suggestions as a searchable training plan.
5. **Push notifications** — "Your analysis is ready" via Firebase.
6. **Comparison view** — side-by-side diff of two sessions to visualise improvement.

---

## Security Notes

- Passwords hashed with BCrypt (strength 10)
- JWT signed with HS256; secret loaded from env, never in source
- Image paths sanitised in `ImageController` to prevent path traversal
- Users can only access their own sessions (enforced in service layer, not just controller)
- No API keys or secrets in source — all via `.env`

---

*Built for Playmotech Ghost Coach Assignment · Role: Backend Engineer*
