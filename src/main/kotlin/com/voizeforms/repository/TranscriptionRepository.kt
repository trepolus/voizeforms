package com.voizeforms.repository

import com.voizeforms.model.Transcription
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transcription operations.
 * Supports both hot flow streaming for real-time transcription chunks
 * and traditional CRUD operations for persistence.
 */
interface TranscriptionRepository {
    
    /**
     * Hot Flow: Get a stream of transcription chunks for a specific session.
     * This is a hot flow that emits real-time transcription chunks as they arrive.
     * Multiple subscribers can listen to the same session stream.
     */
    fun getTranscriptionStream(sessionId: String): Flow<Transcription>
    
    /**
     * Hot Flow: Add a transcription chunk to the stream for a specific session.
     * This will emit the chunk to all subscribers of the session stream.
     */
    suspend fun addTranscriptionChunk(sessionId: String, chunk: String, confidence: Double = 0.0)
    
    /**
     * Persistence: Save a complete transcription to the database.
     * Returns the generated ID of the saved transcription.
     */
    suspend fun saveTranscription(transcription: Transcription): String
    
    /**
     * Persistence: Retrieve a transcription by session ID.
     * Returns null if no transcription found for the session.
     */
    suspend fun getTranscriptionBySessionId(sessionId: String): Transcription?
    
    /**
     * Persistence: Retrieve transcriptions by user ID.
     */
    suspend fun getTranscriptionsByUserId(userId: String): List<Transcription>
    
    /**
     * Session Management: End a transcription session and mark as complete.
     * This stops the hot flow for the session and optionally persists the final result.
     */
    suspend fun endTranscriptionSession(sessionId: String, finalText: String? = null): String?
} 