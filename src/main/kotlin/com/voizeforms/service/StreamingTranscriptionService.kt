package com.voizeforms.service

import com.voizeforms.model.TranscriptionResult
import com.voizeforms.model.SessionInfo
import com.voizeforms.model.Transcription
import com.voizeforms.repository.TranscriptionRepository
import com.voizeforms.repository.MongoTranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


class StreamingTranscriptionService(
    private val repository: TranscriptionRepository
) : TranscriptionService {
    
    private val logger = LoggerFactory.getLogger(StreamingTranscriptionService::class.java)
    private val activeSessions = ConcurrentHashMap<String, SessionInfo>()
    private val sessionChunks = ConcurrentHashMap<String, MutableList<String>>()
    
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
        logger.info("Starting transcription session for user: $userId")
        val sessionId = "session-${UUID.randomUUID()}"
        val sessionInfo = SessionInfo(
            sessionId = sessionId,
            userId = userId,
            isActive = true,
            startTime = System.currentTimeMillis()
        )
        
        activeSessions[sessionId] = sessionInfo
        sessionChunks[sessionId] = mutableListOf()
        
        if (repository is MongoTranscriptionRepository) {
            repository.registerSession(sessionId, userId)
        }
        
        logger.info("Created session with ID: $sessionId")
        return sessionId
    }
    
    /**
     * Process an audio chunk and emit transcription to the hot flow.
     * This simulates speech-to-text processing (mock implementation).
     */
    override suspend fun processAudioChunk(sessionId: String, audioChunk: ByteArray) {
        logger.info("Processing audio chunk for session: $sessionId (${audioChunk.size} bytes)")
        try {
            // Mock transcription logic - convert audio bytes to text
            val transcribedText = when {
                audioChunk.isEmpty() -> "[Error: Unable to process audio]"
                else -> String(audioChunk) 
            }
            
            val confidence = if (transcribedText.startsWith("[Error")) 0.0 else 0.85
            logger.info("Transcribed text for session $sessionId: '$transcribedText' (confidence: $confidence)")
            
            // Add to repository hot flow
            repository.addTranscriptionChunk(sessionId, transcribedText, confidence)
            logger.info("Added chunk to repository for session: $sessionId")
            
            // Store chunk for final text building
            sessionChunks[sessionId]?.add(transcribedText)
            logger.info("Stored chunk in session chunks for: $sessionId")
            
            // Update session state
            activeSessions[sessionId]?.let { session ->
                val updatedSession = session.copy(totalChunks = session.totalChunks + 1)
                activeSessions[sessionId] = updatedSession
                logger.info("Updated session $sessionId - total chunks: ${updatedSession.totalChunks}")
            }
            
        } catch (e: Exception) {
            logger.error("Error processing audio chunk for session $sessionId", e)
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
        logger.info("Ending transcription session: $sessionId with finalText: $finalText")
        
        // Mark session as inactive
        activeSessions[sessionId]?.let { session ->
            val endedSession = session.copy(
                isActive = false,
                endTime = System.currentTimeMillis()
            )
            activeSessions[sessionId] = endedSession
            logger.info("Marked session as inactive: $sessionId")
        } ?: logger.warn("Session not found in active sessions: $sessionId")
        
        // Build final text from all chunks if not provided
        val completeFinalText = finalText ?: run {
            val chunks = sessionChunks[sessionId]
            val combinedText = chunks?.joinToString(" ") { it.trim() }?.trim()
            logger.info("Built final text from ${chunks?.size ?: 0} chunks: '$combinedText'")
            combinedText
        }
        
        // Clean up session chunks
        sessionChunks.remove(sessionId)
        
        // End repository session and get final transcription ID
        val result = repository.endTranscriptionSession(sessionId, completeFinalText)
        logger.info("Repository endTranscriptionSession returned: $result")
        return result
    }
    
    /**
     * Get information about a transcription session.
     */
    override suspend fun getSessionInfo(sessionId: String): SessionInfo? {
        val sessionInfo = activeSessions[sessionId]
        logger.info("Retrieved session info for $sessionId: $sessionInfo")
        return sessionInfo
    }
    
    /**
     * Get all transcriptions for a user from the repository.
     */
    override suspend fun getTranscriptionsByUserId(userId: String): List<Transcription> {
        logger.info("Retrieving transcriptions for user: $userId")
        val transcriptions = repository.getTranscriptionsByUserId(userId)
        logger.info("Found ${transcriptions.size} transcriptions for user $userId")
        return transcriptions
    }
} 