package com.voizeforms.routes

import com.voizeforms.config.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.simpleWebRoutes() {
    // Root endpoint - check authentication status
    get("/") {
        val session = call.sessions.get<UserSession>()
        if (session != null) {
            call.respondRedirect("/dashboard")
        } else {
            val error = call.request.queryParameters["error"]
            val errorMessage = when (error) {
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
                        .demo-btn {
                            background: #6c757d; color: white; padding: 12px 24px; 
                            text-decoration: none; border-radius: 4px; display: inline-block;
                            margin: 20px 10px;
                        }
                    </style>
                </head>
                <body>
                    <h1>🎤 VoizeForms</h1>
                    <p>Real-time Voice-to-Text Form Assistant</p>
                    <a href="/auth/login" class="login-btn">🔐 Login with Google</a>
                    <br>
                    <a href="/dashboard" class="demo-btn">🚀 Demo Mode (No Auth)</a>
                    $errorMessage
                </body>
                </html>
                """.trimIndent()
            )
        }
    }

    // Dashboard - shows different content based on authentication
    get("/dashboard") {
        val session = call.sessions.get<UserSession>()
        val userInfo = if (session != null) {
            """
            <div style="background: #d4edda; padding: 15px; border-radius: 5px; margin-bottom: 20px;">
                <strong>✅ Authenticated as:</strong> ${session.name} (${session.email})
                <a href="/auth/logout" style="float: right; background: #dc3545; color: white; padding: 5px 10px; text-decoration: none; border-radius: 3px;">Logout</a>
            </div>
            """
        } else {
            """
            <div style="background: #fff3cd; padding: 15px; border-radius: 5px; margin-bottom: 20px;">
                <strong>⚠️ Demo Mode:</strong> You are not authenticated. 
                <a href="/auth/login" style="background: #4285f4; color: white; padding: 5px 10px; text-decoration: none; border-radius: 3px;">Login with Google</a>
            </div>
            """
        }
        
        call.respondText(
            contentType = ContentType.Text.Html,
            text = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>VoizeForms - Dashboard</title>
                <style>
                    body { font-family: Arial, sans-serif; padding: 20px; max-width: 800px; margin: 0 auto; }
                    .api-section { margin: 30px 0; padding: 20px; background: #f8f9fa; border-radius: 8px; }
                    code { background: #e9ecef; padding: 2px 4px; border-radius: 3px; }
                </style>
            </head>
            <body>
                <h1>🎤 VoizeForms Dashboard</h1>
                $userInfo
                
                <div class="api-section">
                    <h2>API Endpoints</h2>
                    <ul>
                        <li><strong>GET /health</strong> - Health check</li>
                        <li><strong>POST /api/transcribe</strong> - Voice transcription</li>
                    </ul>
                    
                    <h3>Test Health Check</h3>
                    <code>curl http://localhost:8080/health</code>
                    
                    <h3>Test Transcription</h3>
                    <code>curl -X POST -H "Content-Type: application/octet-stream" --data-binary @audio.wav http://localhost:8080/api/transcribe</code>
                </div>
                
                <p><a href="/">← Back to Home</a></p>
            </body>
            </html>
            """.trimIndent()
        )
    }
} 