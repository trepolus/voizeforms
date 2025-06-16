package com.voizeforms.service

import com.voizeforms.model.TranscriptionResult
import com.voizeforms.model.SessionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class MockTranscriptionService : TranscriptionService {
    override suspend fun transcribe(audioData: ByteArray): Flow<TranscriptionResult> = flow {
        // Simulate processing delay
        delay(500)
        emit(TranscriptionResult("Hello", 0.95))
        delay(300)
        emit(TranscriptionResult("Hello world", 0.98))
        delay(200)
        emit(TranscriptionResult("Hello world, this is a test.", 0.99))
    }
    
    // Simple stub implementations for new methods
    override suspend fun startTranscriptionSession(userId: String): String = "mock-session-123"
    override suspend fun processAudioChunk(sessionId: String, audioChunk: ByteArray) {}
    override fun subscribeToTranscriptionStream(sessionId: String): Flow<TranscriptionResult> = 
        flowOf(TranscriptionResult("Mock result", 0.95))
    override suspend fun endTranscriptionSession(sessionId: String, finalText: String?): String? = "mock-saved-id"
    override suspend fun getSessionInfo(sessionId: String): SessionInfo? = 
        SessionInfo(sessionId, "mock-user", true, System.currentTimeMillis())
} 