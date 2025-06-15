package com.voizeforms.routes

import com.voizeforms.model.UserSession
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
    // Root endpoint - shows login or redirects to instructions
    get("/") {
        val session = call.sessions.get<UserSession>()
        if (session != null) {
            call.respondRedirect("/instructions")
        } else {
            val error = call.request.queryParameters["error"]
            val errorMessage = when (error) {
                "unauthorized" -> "<p style='color: red; margin-top: 20px;'>Your email is not authorized to use this service.</p>"
                "auth_failed" -> "<p style='color: red; margin-top: 20px;'>Authentication failed. Please try again.</p>"
                else -> ""
            }

            call.respondText(
                contentType = ContentType.Text.Html,
                text = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>VoizeForms - Login</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                        .login-btn { 
                            background: #4285f4; color: white; padding: 12px 24px; 
                            text-decoration: none; border-radius: 4px; display: inline-block;
                            margin: 20px 0;
                        }
                        .login-btn:hover { background: #357ae8; }
                    </style>
                </head>
                <body>
                    <h1>🎤 VoizeForms</h1>
                    <p>Real-time Voice-to-Text Form Assistant</p>
                    <a href="/auth/login" class="login-btn">🔐 Login with Google</a>
                    $errorMessage
                </body>
                </html>
                """.trimIndent()
            )
        }
    }

    // Instructions page - shown after successful login
    get("/instructions") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/")
            return@get
        }

        call.respondText(
            contentType = ContentType.Text.Html,
            text = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>VoizeForms - Instructions</title>
                 <style>
                    body { font-family: Arial, sans-serif; padding: 20px; max-width: 800px; margin: 0 auto; }
                    .api-section { margin: 30px 0; padding: 20px; background: #f8f9fa; border-radius: 8px; }
                    code { background: #e9ecef; padding: 2px 4px; border-radius: 3px; }
                     .user-info { background: #d4edda; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <h1>🎤 VoizeForms</h1>
                <div class="user-info">
                    <strong>✅ Authenticated as:</strong> ${session.name} (${session.email})
                    <a href="/auth/logout" style="float: right; background: #dc3545; color: white; padding: 5px 10px; text-decoration: none; border-radius: 3px;">Logout</a>
                </div>
                
                <div class="api-section">
                    <h2>API Usage</h2>
                    <p>You are authenticated. You can now use the transcription API.</p>
                    <h3>Test Transcription</h3>
                    <p>Make a POST request to <code>/api/v1/transcribe</code> with your audio file.</p>
                    <code>curl -X POST -H "Content-Type: application/octet-stream" --data-binary @audio.wav ${
                System.getenv(
                    "BASE_URL"
                )
            }/api/v1/transcribe</code>
                </div>
                
                <p><a href="/auth/logout">← Logout</a></p>
            </body>
            </html>
            """.trimIndent()
        )
    }

    route("/auth") {
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
                        call.respondRedirect("/instructions")
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
