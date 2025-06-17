# 🎤 VoizeForms

> Voice-to-Text API Service with OAuth Authentication

## 🎯 Overview

VoizeForms is a Kotlin/Ktor backend service that provides secure voice transcription capabilities. Built with modern Kotlin features and protected by Google OAuth authentication.

**Live Demo:** [https://voize.lucaskummer.com](https://voize.lucaskummer.com)

## ✨ Current Features

- 🎙️ Voice-to-text transcription API
- 🔒 Google OAuth authentication with email whitelist
- 📊 Streaming transcription responses
- 🐳 Docker deployment ready
- ☁️ Production deployment on Google Cloud Run

## 🚀 Quick Start

### Prerequisites

- JDK 17+
- Docker (optional)
- Google Cloud account for OAuth setup

### Environment Setup

1. **Create Google OAuth credentials:**
   - Go to [Google Cloud Console](https://console.cloud.google.com)
   - Create OAuth 2.0 Client ID
   - Add redirect URI: `http://localhost:8080/auth/callback`

2. **Set environment variables:**
```bash
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
export ALLOWED_EMAILS="your.email@gmail.com"
export BASE_URL="http://localhost:8080"
```

### Run Locally

**Option 1: Gradle**
```bash
./gradlew run
```

**Option 2: Docker**
```bash
docker-compose up --build
```

Visit `http://localhost:8080` to login and get started.

## 🛠️ API Reference

All endpoints are OAuth-protected (session cookie) and live under `/api/v1/*`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/health` | Health check |
| `POST` | `/api/v1/transcribe` | One-shot transcription (cold flow) |
| `POST` | `/api/v1/transcription/session` | Start a new streaming session; returns `{ "sessionId": "…" }` |
| `POST` | `/api/v1/transcription/session/{sessionId}/chunk` | Add an audio/text chunk to the session |
| `DELETE` | `/api/v1/transcription/session/{sessionId}` | End session & persist final text |
| `GET`  | `/api/v1/transcription/stream/{sessionId}` | Server-Sent Events stream of live transcript chunks |
| `GET`  | `/api/v1/transcription/history` | List past transcriptions for the logged-in user |

### Curl snippets
```bash
# Cold flow
curl -X POST --data-binary @audio.wav \
     -H "Content-Type: application/octet-stream" \
     -b "cookie from /auth/login" \
     http://localhost:8080/api/v1/transcribe

# Start a live session
SESSION=$(curl -sb cookies.txt -X POST http://localhost:8080/api/v1/transcription/session | jq -r .sessionId)

# Stream it
curl -N http://localhost:8080/api/v1/transcription/stream/$SESSION
```

## 🔒 Authentication

- Uses Google OAuth 2.0
- Email-based access control via `ALLOWED_EMAILS`
- Session-based authentication after login
- Supports multiple emails: `email1@domain.com,email2@domain.com`

## 🧪 Testing

```bash
./gradlew test
```

## 🚀 Deployment

The service is production-ready with:
- GitHub Actions CI/CD pipeline
- Docker containerization
- Google Cloud Run deployment
- Environment-based configuration
