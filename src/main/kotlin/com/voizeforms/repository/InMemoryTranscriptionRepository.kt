package com.voizeforms.repository

import com.voizeforms.model.Transcription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Pure in-memory implementation of TranscriptionRepository.
 * Used for fast unit testing with no external dependencies.
 * Supports hot flows and session management without persistence.
 */
class InMemoryTranscriptionRepository : TranscriptionRepository {
    
    // Hot Flow Management: Session-specific shared flows for real-time streaming
    private val sessionFlows = ConcurrentHashMap<String, MutableSharedFlow<Transcription>>()
    
    // In-memory storage for testing
    private val storage = ConcurrentHashMap<String, Transcription>()
    private val idGenerator = AtomicLong(1)
    
    /**
     * Hot Flow: Get stream for a specific session.
     * This is a HOT FLOW - it will emit to all subscribers simultaneously.
     */
    override fun getTranscriptionStream(sessionId: String): Flow<Transcription> {
        val sharedFlow = sessionFlows.computeIfAbsent(sessionId) {
            MutableSharedFlow<Transcription>(
                replay = 0,           // Don't replay past emissions
                extraBufferCapacity = 10  // Buffer 10 items if consumers are slow
            )
        }
        
        return sharedFlow.asSharedFlow().filter { it.sessionId == sessionId }
    }
    
    /**
     * Hot Flow: Add chunk to the stream.
     * Emits immediately to all current subscribers of the session.
     */
    override suspend fun addTranscriptionChunk(sessionId: String, chunk: String, confidence: Double) {
        val transcription = Transcription(
            sessionId = sessionId,
            chunk = chunk,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            isComplete = false
        )
        
        // Get or create the shared flow for this session
        val sharedFlow = sessionFlows.computeIfAbsent(sessionId) {
            MutableSharedFlow<Transcription>(
                replay = 0,
                extraBufferCapacity = 10
            )
        }
        
        // Emit to all subscribers (HOT FLOW behavior)
        sharedFlow.emit(transcription)
    }
    
    /**
     * Persistence: Save complete transcription to in-memory storage.
     */
    override suspend fun saveTranscription(transcription: Transcription): String {
        val id = transcription.id ?: idGenerator.getAndIncrement().toString()
        val transcriptionWithId = transcription.copy(id = id)
        storage[id] = transcriptionWithId
        return id
    }
    
    /**
     * Persistence: Retrieve transcription by session ID from in-memory storage.
     */
    override suspend fun getTranscriptionBySessionId(sessionId: String): Transcription? {
        return storage.values.find { it.sessionId == sessionId }
    }
    
    /**
     * Persistence: Retrieve transcriptions by user ID from in-memory storage.
     */
    override suspend fun getTranscriptionsByUserId(userId: String): List<Transcription> {
        return storage.values.filter { it.userId == userId }
    }
    
    /**
     * Session Management: End transcription session and clean up.
     */
    override suspend fun endTranscriptionSession(sessionId: String, finalText: String?): String? {
        // Remove the hot flow for this session (clean up memory)
        sessionFlows.remove(sessionId)
        
        // Optionally save the final complete transcription
        return finalText?.let { text ->
            val finalTranscription = Transcription(
                sessionId = sessionId,
                chunk = text,
                confidence = 1.0,
                isComplete = true
            )
            saveTranscription(finalTranscription)
        }
    }
} 