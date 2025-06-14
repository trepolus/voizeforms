package com.voizeforms.unit.service

import com.voizeforms.model.TranscriptionResult
import com.voizeforms.service.TranscriptionService
import com.voizeforms.service.MockTranscriptionService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TranscriptionServiceTest {
    private val service: TranscriptionService = MockTranscriptionService()

    @Test
    fun `should return transcription result`() = runTest {
        // Given
        val audioData = ByteArray(10) { 0 }

        // When
        val result = service.transcribe(audioData).first()

        // Then
        assertNotNull(result)
        assertEquals("Hello", result.text)
        assertEquals(0.95, result.confidence)
    }

    @Test
    fun `should return multiple transcription results`() = runTest {
        // Given
        val audioData = ByteArray(10) { 0 }
        val results = mutableListOf<TranscriptionResult>()

        // When
        service.transcribe(audioData).collect { result ->
            results.add(result)
        }

        // Then
        assertEquals(3, results.size)
        assertEquals("Hello", results[0].text)
        assertEquals("Hello world", results[1].text)
        assertEquals("Hello world, this is a test.", results[2].text)
    }
} 