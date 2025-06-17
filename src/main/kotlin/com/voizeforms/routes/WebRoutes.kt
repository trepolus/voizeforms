package com.voizeforms.routes

import com.voizeforms.model.UserSession
import com.voizeforms.templates.HtmlTemplates
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class GoogleUserInfo(
    val id: String,
    val name: String,
    val email: String,
    val picture: String? = null
)

fun Route.webRoutes(httpClient: HttpClient) {
    // Root endpoint - shows login or redirects to dashboard
    get("/") {
        val session = call.sessions.get<UserSession>()
        if (session != null) {
            call.respondRedirect("/dashboard")
        } else {
            val error = call.request.queryParameters["error"]
            val errorMessage = when (error) {
                "unauthorized" -> "<p style='color: red; margin-top: 20px;'>Your email is not authorized to use this service.</p>"
                "auth_failed" -> "<p style='color: red; margin-top: 20px;'>Authentication failed. Please try again.</p>"
                else -> ""
            }

            call.respondText(
                contentType = ContentType.Text.Html,
                text = HtmlTemplates.loginPage(errorMessage)
            )
        }
    }

    // Dashboard page - interactive voice note interface
    get("/dashboard") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/")
            return@get
        }

        val baseUrl = System.getenv("BASE_URL") ?: "http://localhost:8080"
        call.respondText(
            contentType = ContentType.Text.Html,
            text = HtmlTemplates.dashboardPage(session, baseUrl)
        )
    }

    route("/auth") {
        // Login route and callback - requires authentication
        authenticate("google-oauth") {
            get("/login") {
                // This will automatically redirect to Google OAuth
            }
            
            // Callback route - handles OAuth response from Google
            get("/callback") {
                call.application.log.info("OAuth callback received - query params: ${call.request.queryParameters.entries()}")
                
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                call.application.log.info("OAuth principal: ${if (principal != null) "present" else "null"}")

                if (principal != null) {
                    try {
                        call.application.log.info("Attempting to get user info from Google with access token")
                        
                        // Get user info from Google
                        val userInfo: GoogleUserInfo = httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                            headers {
                                append(HttpHeaders.Authorization, "Bearer ${principal.accessToken}")
                            }
                        }.body()

                        call.application.log.info("Retrieved user info for: ${userInfo.email}")

                        // Get allowed emails from environment
                        val allowedEmails = System.getenv("ALLOWED_EMAILS")
                            ?.split(',', ';')
                            ?.map { it.trim().lowercase() }
                            ?.filter { it.isNotEmpty() }
                            ?: throw IllegalStateException("ALLOWED_EMAILS environment variable is required")

                        call.application.log.info("Allowed emails: $allowedEmails")

                        // Validate email
                        if (!allowedEmails.contains(userInfo.email.lowercase())) {
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
                        call.application.log.info("User authenticated successfully: ${userInfo.email}")
                        call.respondRedirect("/dashboard")
                    } catch (e: Exception) {
                        call.application.log.error("Failed to get user info from Google", e)
                        call.respondRedirect("/?error=auth_failed")
                    }
                } else {
                    call.application.log.warn("OAuth callback received without principal")
                    call.respondRedirect("/?error=auth_failed")
                }
            }
        }
        
        // Logout endpoint - outside authentication
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
    }
}
