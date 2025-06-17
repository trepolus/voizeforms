package com.voizeforms.plugins

import com.voizeforms.model.UserSession
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

fun Application.configureAuthentication() {
    val googleClientId = System.getenv("GOOGLE_CLIENT_ID")
        ?: throw IllegalStateException("GOOGLE_CLIENT_ID environment variable is required")
    val googleClientSecret = System.getenv("GOOGLE_CLIENT_SECRET")
        ?: throw IllegalStateException("GOOGLE_CLIENT_SECRET environment variable is required")
    val baseUrl = System.getenv("BASE_URL") ?: "http://localhost:8080"
    val redirectUrl = "$baseUrl/auth/callback"

    log.info("OAuth callback redirected: $redirectUrl")

    val allowedEmails = System.getenv("ALLOWED_EMAILS")
        ?.splitToSequence(',',';')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toList()
        ?: throw IllegalStateException("ALLOWED_EMAILS environment variable is required")

    if (allowedEmails.isEmpty()) {
        throw IllegalStateException("ALLOWED_EMAILS environment variable is empty")
    }

    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 86400 // 24 hours
            cookie.secure = baseUrl.startsWith("https")
            cookie.extensions["SameSite"] = "Lax"
        }
    }

    install(Authentication) {
        oauth("google-oauth") {
            urlProvider = { redirectUrl }
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
        session<UserSession>("user-session") {
            validate { session ->
                if (session.userId.isNotEmpty() && session.email.isNotEmpty()) {
                    session
                } else {
                    null
                }
            }
            challenge {
                call.respondRedirect("/")
            }
        }
    }
} 