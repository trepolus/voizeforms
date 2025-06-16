package com.voizeforms.service

import com.voizeforms.model.TranscriptionResult
import com.voizeforms.model.SessionInfo
import kotlinx.coroutines.flow.Flow

interface TranscriptionService {
    suspend fun transcribe(audioData: ByteArray): Flow<TranscriptionResult>
    
    // New hot flow and session management methods
    suspend fun startTranscriptionSession(userId: String): String
    suspend fun processAudioChunk(sessionId: String, audioChunk: ByteArray)
    fun subscribeToTranscriptionStream(sessionId: String): Flow<TranscriptionResult>
    suspend fun endTranscriptionSession(sessionId: String, finalText: String? = null): String?
    suspend fun getSessionInfo(sessionId: String): SessionInfo?
} 