package com.voizeforms.routes

import com.voizeforms.model.TranscriptionResult
import com.voizeforms.model.UserSession
import com.voizeforms.service.TranscriptionService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SessionResponse(val sessionId: String)

@Serializable
data class SavedTranscriptionResponse(val savedId: String?)

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

fun Route.sessionManagementRoutes(transcriptionService: TranscriptionService) {
    authenticate("google-oauth") {
        route("/api/v1/transcription/session") {
            // Start new transcription session
            post {
                try {
                    val userSession = call.sessions.get<UserSession>()
                    val userId = userSession?.userId ?: "anonymous"
                    
                    val sessionId = transcriptionService.startTranscriptionSession(userId)
                    call.respond(HttpStatusCode.Created, SessionResponse(sessionId))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error starting session: ${e.message}")
                }
            }
            
            // Add audio chunk to session
            post("/{sessionId}/chunk") {
                try {
                    val sessionId = call.parameters["sessionId"]
                    if (sessionId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Session ID is required")
                        return@post
                    }
                    
                    if (call.request.contentLength() == 0L) {
                        call.respond(HttpStatusCode.BadRequest, "No audio data provided")
                        return@post
                    }
                    
                    val audioData = call.receive<ByteArray>()
                    transcriptionService.processAudioChunk(sessionId, audioData)
                    call.respond(HttpStatusCode.OK, "Chunk processed")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error processing chunk: ${e.message}")
                }
            }
            
            // End transcription session
            delete("/{sessionId}") {
                try {
                    val sessionId = call.parameters["sessionId"]
                    if (sessionId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Session ID is required")
                        return@delete
                    }
                    
                    val finalText = call.request.queryParameters["finalText"]
                    val savedId = transcriptionService.endTranscriptionSession(sessionId, finalText)
                    call.respond(HttpStatusCode.OK, SavedTranscriptionResponse(savedId))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error ending session: ${e.message}")
                }
            }
        }
        
        // Get transcription history
        get("/api/v1/transcription/history") {
            try {
                val userSession = call.sessions.get<UserSession>()
                val userId = userSession?.userId ?: "anonymous"
                
                // For now, return empty list - we'll implement this when we have repository method
                call.respond(HttpStatusCode.OK, emptyList<String>())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error retrieving history: ${e.message}")
            }
        }
    }
} 