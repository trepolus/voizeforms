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
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import com.voizeforms.util.Routes
import org.koin.ktor.ext.getKoin
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import com.voizeforms.model.ErrorInfo
import com.voizeforms.model.ErrorResponse

fun Route.transcriptionRoutes() {
    val transcriptionService = application.getKoin().get<TranscriptionService>()
    authenticate("user-session") {
        route(Routes.TRANSCRIBE) {
            post {
                try {
                    if (call.request.contentLength() == 0L) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorInfo("NO_AUDIO", "No audio data provided"))
                        )
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
        route(Routes.TRANSCRIPTION_STREAM) {
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

fun Route.sessionManagementRoutes() {
    val transcriptionService = application.getKoin().get<TranscriptionService>()
    val logger = LoggerFactory.getLogger("SessionManagementRoutes")

    route("${Routes.API_V1}/transcription") {
        authenticate("user-session") {

            // Start new transcription session
            post("/session") {
                logger.info("POST /session - Starting new transcription session")
                try {
                    val userSession = call.sessions.get<UserSession>()
                    val userId = userSession?.email ?: "anonymous"
                    logger.info("Starting session for user: $userId (from session: $userSession)")

                    val sessionId = transcriptionService.startTranscriptionSession(userId)
                    logger.info("Created session with ID: $sessionId")

                    call.respond(mapOf("sessionId" to sessionId))
                } catch (e: Exception) {
                    logger.error("Error starting transcription session", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to start session"))
                }
            }

            // Add audio/text chunk to session
            post("/session/{sessionId}/chunk") {
                logger.info("POST /session/{sessionId}/chunk - Adding chunk to session")
                try {
                    val sessionId = call.parameters["sessionId"]
                    if (sessionId == null) {
                        logger.warn("Session ID missing in chunk request")
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID required"))
                        return@post
                    }

                    logger.info("Processing chunk for session: $sessionId")

                    val requestBody = call.receiveText()
                    logger.info("Received chunk data: '${requestBody.take(100)}${if (requestBody.length > 100) "..." else ""}'")

                    if (requestBody.isBlank()) {
                        logger.warn("Empty chunk data received for session: $sessionId")
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Audio data required"))
                        return@post
                    }

                    // Convert text to bytes for processing (simulation)
                    val audioChunk = requestBody.toByteArray()
                    transcriptionService.processAudioChunk(sessionId, audioChunk)
                    logger.info("Successfully processed chunk for session: $sessionId")

                    call.respond(HttpStatusCode.OK, mapOf("status" to "chunk processed"))
                } catch (e: Exception) {
                    logger.error("Error processing audio chunk", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to process chunk"))
                }
            }

            // End session and save final transcription
            delete("/session/{sessionId}") {
                logger.info("DELETE /session/{sessionId} - Ending transcription session")
                try {
                    val sessionId = call.parameters["sessionId"]
                    if (sessionId == null) {
                        logger.warn("Session ID missing in delete request")
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID required"))
                        return@delete
                    }

                    logger.info("Ending session: $sessionId")

                    // Get session info before ending
                    val sessionInfo = transcriptionService.getSessionInfo(sessionId)
                    logger.info("Session info before ending: $sessionInfo")

                    val finalTranscriptionId = transcriptionService.endTranscriptionSession(sessionId)
                    logger.info("Session ended, final transcription ID: $finalTranscriptionId")

                    val responseMessage = if (finalTranscriptionId != null && finalTranscriptionId != "unknown") {
                        "Session saved with ID: $finalTranscriptionId"
                    } else {
                        "Session ended but save failed - ID: $finalTranscriptionId"
                    }

                    logger.info("Responding with: $responseMessage")
                    call.respond(mapOf("message" to responseMessage, "savedId" to finalTranscriptionId))
                } catch (e: Exception) {
                    logger.error("Error ending transcription session", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to end session"))
                }
            }

            // Get transcription history
            get("/history") {
                logger.info("GET /history - Retrieving transcription history")
                try {
                    val userSession = call.sessions.get<UserSession>()
                    val userId = userSession?.email ?: "anonymous"
                    logger.info("Retrieving history for user: $userId (from session: $userSession)")

                    val transcriptions = transcriptionService.getTranscriptionsByUserId(userId)
                    logger.info("Found ${transcriptions.size} transcriptions for user: $userId")

                    if (transcriptions.isEmpty()) {
                        logger.info("No transcriptions found, returning empty message")
                    } else {
                        logger.info("Returning transcriptions: ${transcriptions.map { it.sessionId }}")
                    }

                    call.respond(transcriptions)
                } catch (e: Exception) {
                    logger.error("Error retrieving transcription history", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to retrieve history"))
                }
            }
        }
    }
}

fun Route.transcriptionRoutes(transcriptionService: TranscriptionService) {
    val koin = this.application.getKoin()
    if (koin.getOrNull<TranscriptionService>() == null) {
        loadKoinModules(module { single { transcriptionService } })
    }
    transcriptionRoutes()
}

fun Route.sessionManagementRoutes(transcriptionService: TranscriptionService) {
    val koin = this.application.getKoin()
    if (koin.getOrNull<TranscriptionService>() == null) {
        loadKoinModules(module { single { transcriptionService } })
    }
    sessionManagementRoutes()
} 