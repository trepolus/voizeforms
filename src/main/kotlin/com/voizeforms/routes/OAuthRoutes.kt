package com.voizeforms.routes

import com.voizeforms.config.UserSession
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class GoogleUserInfo(
    val id: String,
    val name: String,
    val email: String,
    val picture: String? = null
)

fun Route.oauthRoutes(httpClient: HttpClient) {
    route("/auth") {
        // Login endpoint - redirects to Google
        authenticate("google-oauth") {
            get("/login") {
                // This will automatically redirect to Google OAuth
            }

            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                
                if (principal != null) {
                    try {
                        // Get user info from Google
                        val userInfo: GoogleUserInfo = httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                            headers {
                                append(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
                            }
                        }.body()

                        // Get allowed emails from environment
                        val allowedEmails = System.getenv("ALLOWED_EMAILS")?.split(",")?.map { it.trim() }
                            ?: throw IllegalStateException("ALLOWED_EMAILS environment variable is required")

                        // Validate email
                        if (!allowedEmails.contains(userInfo.email)) {
                            call.application.log.warn("Unauthorized access attempt from email: ${userInfo.email}")
                            call.respondRedirect("/?error=unauthorized")
                            return@get
                        }

                        // Create user session
                        val userSession = UserSession(
                            userId = userInfo.id,
                            name = userInfo.name,
                            email = userInfo.email
                        )
                        
                        call.sessions.set(userSession)
                        call.respondRedirect("/dashboard")
                    } catch (e: Exception) {
                        call.application.log.error("Failed to get user info from Google", e)
                        call.respondRedirect("/?error=auth_failed")
                    }
                } else {
                    call.respondRedirect("/?error=auth_failed")
                }
            }
        }

        // Logout endpoint
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
    }
} 