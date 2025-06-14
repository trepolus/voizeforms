package com.voizeforms.integration.routes

import com.voizeforms.service.MockTranscriptionService
import com.voizeforms.routes.transcriptionRoutes
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.server.routing.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*

class TranscriptionRoutesTest {
    @Test
    fun `should return 200 when valid audio data is provided`() = testApplication {
        // Given
        val service = MockTranscriptionService()
        application {
            routing {
                transcriptionRoutes(service)
            }
        }
        val audioData = ByteArray(10) { 0 }

        // When
        val response = client.post("/api/transcribe") {
            setBody(audioData)
            header(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
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
            routing {
                transcriptionRoutes(service)
            }
        }

        // When
        val response = client.post("/api/transcribe") {
            header(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString())
        }

        // Then
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
} 