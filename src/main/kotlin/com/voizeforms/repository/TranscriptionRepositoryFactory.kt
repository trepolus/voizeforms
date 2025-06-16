package com.voizeforms.repository

import com.voizeforms.config.MongoConfig

/**
 * Factory for creating TranscriptionRepository instances.
 * Provides clean dependency injection without boolean flags.
 */
object TranscriptionRepositoryFactory {
    
    /**
     * Create repository for production use with real MongoDB.
     */
    fun createMongoRepository(): TranscriptionRepository {
        return MongoTranscriptionRepository(MongoConfig.getDatabase())
    }
    
    /**
     * Create repository for unit testing with in-memory storage.
     */
    fun createInMemoryRepository(): TranscriptionRepository {
        return InMemoryTranscriptionRepository()
    }
    
    /**
     * Create repository based on environment.
     * Checks system property to determine which implementation to use.
     */
    fun createRepository(): TranscriptionRepository {
        return when (System.getProperty("repository.type")) {
            "inmemory" -> createInMemoryRepository()
            else -> createMongoRepository()
        }
    }
} 