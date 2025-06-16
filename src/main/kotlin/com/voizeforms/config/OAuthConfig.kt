package com.voizeforms.config

import com.voizeforms.model.UserSession
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*

fun Application.configureOAuth() {
    // Get OAuth credentials from environment
    val googleClientId = System.getenv("GOOGLE_CLIENT_ID")
        ?: throw IllegalStateException("GOOGLE_CLIENT_ID environment variable is required")
    val googleClientSecret = System.getenv("GOOGLE_CLIENT_SECRET")
        ?: throw IllegalStateException("GOOGLE_CLIENT_SECRET environment variable is required")
    val baseUrl = System.getenv("BASE_URL") ?: "http://localhost:8080"

    // Get allowed emails from environment
    System.getenv("ALLOWED_EMAILS")?.split(",")?.map { it.trim() }
        ?: throw IllegalStateException("ALLOWED_EMAILS environment variable is required")

    // Configure sessions
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 86400 // 24 hours
            // Set secure flag for HTTPS in production
            cookie.secure = baseUrl.startsWith("https")
            // Set SameSite to Lax for OAuth compatibility
            cookie.extensions["SameSite"] = "Lax"
        }
    }

    // Configure OAuth
    install(Authentication) {
        oauth("google-oauth") {
            urlProvider = { "$baseUrl/auth/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = googleClientId,
                    clientSecret = googleClientSecret,
                    defaultScopes = listOf(
                        "https://www.googleapis.com/auth/userinfo.profile",
                        "https://www.googleapis.com/auth/userinfo.email"
                    )
                )
            }
            client = HttpClient(CIO)
        }
    }
} 