package com.voizeforms

import com.voizeforms.config.MongoConfig
import com.voizeforms.config.configureOAuth
import com.voizeforms.repository.TranscriptionRepositoryFactory
import com.voizeforms.routes.healthRoutes
import com.voizeforms.routes.transcriptionRoutes
import com.voizeforms.routes.webRoutes
import com.voizeforms.service.StreamingTranscriptionService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Initialize MongoDB connection
    MongoConfig.init(environment.config)

    // Configure JSON serialization
    install(ContentNegotiation) {
        json()
    }

    // Configure CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true

        // Allow production domain and localhost
        allowHost("voize.lucaskummer.com", schemes = listOf("https"))
        allowHost("localhost:8080", schemes = listOf("http"))
    }

    // Configure status pages for better error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, "Internal server error")
        }
    }

    // Configure OAuth and sessions
    configureOAuth()

    // Initialize services with dependency injection
    val transcriptionRepository = TranscriptionRepositoryFactory.createMongoRepository()
    val transcriptionService = StreamingTranscriptionService(transcriptionRepository)
    val httpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }

    // Configure routing
    routing {
        healthRoutes()
        webRoutes(httpClient)
        transcriptionRoutes(transcriptionService)
    }
}
