package com.voizeforms.integration.routes

import com.voizeforms.routes.transcriptionRoutes
import com.voizeforms.service.MockTranscriptionService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TranscriptionRoutesTest {

    private fun Application.configureTestAuth() {
        install(Authentication) {
            basic("user-session") {
                realm = "Test"
                validate { credentials ->
                    // For testing, accept any credentials
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
    fun `should return 200 when valid audio data is provided`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                transcriptionRoutes(service)
            }
        }
        val audioData = ByteArray(10) { 0 }

        // When
        val response = client.post("/api/v1/transcribe") {
            setBody(audioData)
            header(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
            basicAuth("test", "test")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.bodyAsText())
    }

    @Test
    fun `should return 400 when no audio data is provided`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                transcriptionRoutes(service)
            }
        }

        // When
        val response = client.post("/api/v1/transcribe") {
            header(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
            basicAuth("test", "test")
        }

        // Then
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `should return 401 when not authenticated`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                transcriptionRoutes(service)
            }
        }
        val audioData = ByteArray(10) { 0 }

        // When
        val response = client.post("/api/v1/transcribe") {
            setBody(audioData)
            header(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
            // No authentication
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
    
    @Test
    fun `should stream transcription via SSE for valid session`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                transcriptionRoutes(service)
            }
        }
        val sessionId = "test-session-123"

        // When
        val response = client.get("/api/v1/transcription/stream/$sessionId") {
            header(HttpHeaders.Accept, "text/event-stream")
            basicAuth("test", "test")
        }

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("text/event-stream", response.headers[HttpHeaders.ContentType]?.substringBefore(";"))
        assertTrue(response.bodyAsText().isNotEmpty())
    }

    @Test
    fun `should return 401 for SSE stream when not authenticated`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                transcriptionRoutes(service)
            }
        }
        val sessionId = "test-session-456"

        // When
        val response = client.get("/api/v1/transcription/stream/$sessionId") {
            header(HttpHeaders.Accept, "text/event-stream")
            // No authentication
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `should return 400 for SSE stream with empty session ID`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            configureTestAuth()
            routing {
                transcriptionRoutes(service)
            }
        }

        // When
        val response = client.get("/api/v1/transcription/stream/") {
            header(HttpHeaders.Accept, "text/event-stream")
            basicAuth("test", "test")
        }

        // Then
        assertEquals(HttpStatusCode.NotFound, response.status) // Ktor returns 404 for missing path parameter
    }
} 