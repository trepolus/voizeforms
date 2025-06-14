package com.voizeforms.service

import com.voizeforms.model.TranscriptionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
} 