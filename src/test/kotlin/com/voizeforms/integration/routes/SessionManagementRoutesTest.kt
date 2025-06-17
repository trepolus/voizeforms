package com.voizeforms.integration.routes

import com.voizeforms.routes.sessionManagementRoutes
import com.voizeforms.service.MockTranscriptionService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionManagementRoutesTest {

    private fun Application.configureTestAuth() {
        install(ContentNegotiation) {
            json()
        }
        install(Sessions) {
            cookie<com.voizeforms.model.UserSession>("user_session")
        }
        install(Authentication) {
            basic("user-session") {
                realm = "Test"
                validate { credentials ->
                    if (credentials.name == "test" && credentials.password == "test") {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }
    }

    @Test
    fun `should start new transcription session for authenticated user`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                sessionManagementRoutes(service)
            }
        }

        // When
        val response = client.post("/api/v1/transcription/session") {
            basicAuth("test", "test")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        assertNotNull(json["sessionId"]?.jsonPrimitive?.content)
        assertTrue(json["sessionId"]?.jsonPrimitive?.content?.isNotEmpty() == true)
    }

    @Test
    fun `should return 401 when starting session without authentication`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                sessionManagementRoutes(service)
            }
        }

        // When
        val response = client.post("/api/v1/transcription/session") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            // No authentication
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `should accept audio chunk for valid session`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                sessionManagementRoutes(service)
            }
        }
        val sessionId = "test-session-123"

        // When
        val response = client.post("/api/v1/transcription/session/$sessionId/chunk") {
            basicAuth("test", "test")
            header(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            setBody("Hello world test")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `should return 400 for empty audio chunk`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                sessionManagementRoutes(service)
            }
        }
        val sessionId = "test-session-456"

        // When
        val response = client.post("/api/v1/transcription/session/$sessionId/chunk") {
            basicAuth("test", "test")
            header(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
        }

        // Then
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `should end session and return saved transcription ID`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                sessionManagementRoutes(service)
            }
        }
        val sessionId = "test-session-789"

        // When
        val response = client.delete("/api/v1/transcription/session/$sessionId") {
            basicAuth("test", "test")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Session saved with ID:"))
    }

    @Test
    fun `should get user transcription history`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                sessionManagementRoutes(service)
            }
        }

        // When
        val response = client.get("/api/v1/transcription/history") {
            basicAuth("test", "test")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("application/json", response.headers[HttpHeaders.ContentType]?.substringBefore(";"))
    }
} 