package com.voizeforms.routes

import com.voizeforms.model.TranscriptionResult
import com.voizeforms.service.TranscriptionService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Route.transcriptionRoutes(transcriptionService: TranscriptionService) {
    authenticate("google-oauth") {
        route("/api/v1/transcribe") {
            post {
                try {
                    // Check if body is empty
                    if (call.request.contentLength() == 0L) {
                        call.respond(HttpStatusCode.BadRequest, "No audio data provided")
                        return@post
                    }

                    val audioData = call.receive<ByteArray>()
                    call.response.cacheControl(CacheControl.NoCache(null))

                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                        transcriptionService.transcribe(audioData).collect { result ->
                            write(Json.encodeToString(TranscriptionResult.serializer(), result))
                            write("\n")
                            flush()
                        }
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error processing audio: ${e.message}")
                }
            }
        }
        
        // SSE streaming route for session-based transcription
        route("/api/v1/transcription/stream") {
            get("/{sessionId}") {
                try {
                    val sessionId = call.parameters["sessionId"]
                    if (sessionId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Session ID is required")
                        return@get
                    }

                    call.response.cacheControl(CacheControl.NoCache(null))

                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                        transcriptionService.subscribeToTranscriptionStream(sessionId).collect { result ->
                            // SSE format: data: <JSON>\n\n
                            write("data: ")
                            write(Json.encodeToString(TranscriptionResult.serializer(), result))
                            write("\n\n")
                            flush()
                        }
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error streaming transcription: ${e.message}")
                }
            }
        }
    }
} 