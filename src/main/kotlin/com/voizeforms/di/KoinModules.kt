package com.voizeforms.di

import com.voizeforms.repository.TranscriptionRepository
import com.voizeforms.repository.TranscriptionRepositoryFactory
import com.voizeforms.service.StreamingTranscriptionService
import com.voizeforms.service.TranscriptionService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import org.koin.dsl.module

val repositoryModule = module {
    single<TranscriptionRepository> { TranscriptionRepositoryFactory.createMongoRepository() }
}

val serviceModule = module {
    single<TranscriptionService> { StreamingTranscriptionService(get()) }
}

val httpClientModule = module {
    single<HttpClient> {
        HttpClient(CIO) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
        }
    }
}

val appModules = listOf(repositoryModule, serviceModule, httpClientModule) 