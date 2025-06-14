package com.voizeforms.routes

import com.voizeforms.model.TranscriptionResult
import com.voizeforms.service.TranscriptionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import io.ktor.http.content.*
import io.ktor.server.plugins.BadRequestException

fun Route.transcriptionRoutes(transcriptionService: TranscriptionService) {
    route("/api/transcribe") {
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
} 