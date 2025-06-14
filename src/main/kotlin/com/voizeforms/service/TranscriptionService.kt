package com.voizeforms.service

import com.voizeforms.model.TranscriptionResult
import kotlinx.coroutines.flow.Flow

interface TranscriptionService {
    suspend fun transcribe(audioData: ByteArray): Flow<TranscriptionResult>
} 