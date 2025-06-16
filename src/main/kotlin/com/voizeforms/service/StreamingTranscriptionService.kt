package com.voizeforms.service

import com.voizeforms.model.TranscriptionResult
import com.voizeforms.model.SessionInfo
import com.voizeforms.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Streaming implementation of TranscriptionService that supports hot flows
 * and real-time transcription session management.
 */
class StreamingTranscriptionService(
    private val repository: TranscriptionRepository
) : TranscriptionService {
    
    // Session state management
    private val activeSessions = ConcurrentHashMap<String, SessionInfo>()
    
    /**
     * Legacy method for backward compatibility.
     * For new implementations, use session-based methods.
     */
    override suspend fun transcribe(audioData: ByteArray): Flow<TranscriptionResult> {
        // Create a temporary session for legacy API
        val tempSessionId = startTranscriptionSession("legacy-user")
        processAudioChunk(tempSessionId, audioData)
        return subscribeToTranscriptionStream(tempSessionId)
    }
    
    /**
     * Start a new transcription session for a user.
     * Returns a unique session ID that can be used for streaming.
     */
    override suspend fun startTranscriptionSession(userId: String): String {
        val sessionId = "session-${UUID.randomUUID()}"
        val sessionInfo = SessionInfo(
            sessionId = sessionId,
            userId = userId,
            isActive = true,
            startTime = System.currentTimeMillis()
        )
        
        activeSessions[sessionId] = sessionInfo
        return sessionId
    }
    
    /**
     * Process an audio chunk and emit transcription to the hot flow.
     * This simulates speech-to-text processing (mock implementation).
     */
    override suspend fun processAudioChunk(sessionId: String, audioChunk: ByteArray) {
        try {
            // Mock transcription logic - convert audio bytes to text
            val transcribedText = when {
                audioChunk.isEmpty() -> "[Error: Unable to process audio]"
                else -> String(audioChunk) // Simple mock: treat bytes as text
            }
            
            val confidence = if (transcribedText.startsWith("[Error")) 0.0 else 0.85
            
            // Add to repository hot flow
            repository.addTranscriptionChunk(sessionId, transcribedText, confidence)
            
            // Update session state
            activeSessions[sessionId]?.let { session ->
                val updatedSession = session.copy(totalChunks = session.totalChunks + 1)
                activeSessions[sessionId] = updatedSession
            }
            
        } catch (e: Exception) {
            // Handle errors gracefully
            repository.addTranscriptionChunk(sessionId, "[Error: Unable to process audio]", 0.0)
        }
    }
    
    /**
     * Subscribe to the transcription stream for a session.
     * Converts repository Transcription objects to TranscriptionResult.
     */
    override fun subscribeToTranscriptionStream(sessionId: String): Flow<TranscriptionResult> {
        return repository.getTranscriptionStream(sessionId)
            .map { transcription ->
                TranscriptionResult(
                    text = transcription.chunk,
                    confidence = transcription.confidence,
                    timestamp = transcription.timestamp
                )
            }
    }
    
    /**
     * End a transcription session and optionally save final result.
     * Returns the ID of the saved final transcription.
     */
    override suspend fun endTranscriptionSession(sessionId: String, finalText: String?): String? {
        // Mark session as inactive
        activeSessions[sessionId]?.let { session ->
            val endedSession = session.copy(
                isActive = false,
                endTime = System.currentTimeMillis()
            )
            activeSessions[sessionId] = endedSession
        }
        
        // End repository session and get final transcription ID
        return repository.endTranscriptionSession(sessionId, finalText)
    }
    
    /**
     * Get information about a transcription session.
     */
    override suspend fun getSessionInfo(sessionId: String): SessionInfo? {
        return activeSessions[sessionId]
    }
} 