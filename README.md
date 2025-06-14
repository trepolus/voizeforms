# 🎤 VoizeForms

> Real-time Voice-to-Text Form Assistant powered by Kotlin 2.1+ and Ktor

## 🎯 Overview

VoizeForms is a modern backend service that helps frontline workers streamline their workflows by converting voice input into structured form data. Built with Kotlin 2.1+ and Ktor, it provides real-time transcription capabilities with both cold and hot flow processing.

## ✨ Features

- 🎙️ Real-time voice-to-text transcription
- 🔄 Dual flow processing:
  - Cold flow: On-demand transcription streams
  - Hot flow: Live session updates
- 📝 Structured form data storage
- 🔌 WebSocket/SSE support for live updates
- 🐳 Dockerized deployment
- 🔒 OAuth authentication support
- 📊 MongoDB integration for data persistence

## 🚀 Quick Start

### Prerequisites

- JDK 17 or higher
- Docker and Docker Compose
- MongoDB (included in Docker setup)
- Google Cloud Platform account (for OAuth)

### Environment Setup

1. Set up Google OAuth credentials:
   - Go to [Google Cloud Console](https://console.cloud.google.com)
   - Create a new project or select an existing one
   - Enable the Google+ API
   - Go to Credentials → Create Credentials → OAuth Client ID
   - Set up OAuth consent screen
   - Add authorized redirect URI: `http://localhost:8080/auth/callback` (for local development)
   - Copy the Client ID and Client Secret

2. Configure environment variables:
```bash
# Required OAuth configuration
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
export ALLOWED_EMAILS="user1@example.com,user2@example.com"  # Comma-separated list of allowed emails
export BASE_URL="http://localhost:8080"  # Your application's base URL
```

### Local Development

1. Clone the repository:
```bash
git clone https://github.com/yourusername/voizeforms.git
cd voizeforms
```

2. Start the development environment:
```bash
./gradlew run
```

### Docker Deployment

1. Build and run with Docker Compose:
```bash
docker-compose up --build
```

The service will be available at `http://localhost:8080`

## 🛠️ API Endpoints

- `GET /health` - Health check endpoint
- `POST /api/transcribe` - Submit audio for transcription (cold flow)
- `GET /auth/login` - Initiate OAuth login
- `GET /auth/callback` - OAuth callback endpoint
- `GET /auth/logout` - Logout endpoint

## 🔒 OAuth Configuration

The application uses Google OAuth for authentication. To configure:

1. Set up Google OAuth credentials as described in Environment Setup
2. Configure the allowed emails using the `ALLOWED_EMAILS` environment variable:
   - Single email: `export ALLOWED_EMAILS="user@example.com"`
   - Multiple emails: `export ALLOWED_EMAILS="user1@example.com,user2@example.com,user3@example.com"`
   - No spaces between emails, just commas
   - Case-sensitive matching

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific test suite
./gradlew test --tests "com.voizeforms.unit.*"
```

## 📚 Documentation

Detailed API documentation and database schema can be found in `voizeforms.md`.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

MIT License

Copyright (c) 2025 VoizeForms

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## 🛠️ CI

This project uses **GitHub Actions** for continuous integration. Every push or pull request against `main` triggers the workflow located at `.github/workflows/ci.yml`, which:

1. Sets up JDK 21 with Gradle dependency caching
2. Runs `./gradlew clean build` (compile + unit tests)
3. Builds the Docker image to ensure the Dockerfile stays valid

You can extend the workflow later to push container images or deploy automatically.

