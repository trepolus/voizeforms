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

All API endpoints require OAuth authentication.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Health check |
| `GET` | `/` | Login page |
| `GET` | `/auth/login` | Start OAuth flow |
| `GET` | `/auth/logout` | Logout |
| `POST` | `/api/v1/transcribe` | Submit audio for transcription |

### Example Usage

After authentication, submit audio for transcription:

```bash
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @audio.wav \
  http://localhost:8080/api/v1/transcribe
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
