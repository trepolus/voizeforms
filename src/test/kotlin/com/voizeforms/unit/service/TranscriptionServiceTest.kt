package com.voizeforms.unit.service

import com.voizeforms.model.Transcription
import com.voizeforms.model.TranscriptionResult
import com.voizeforms.repository.TranscriptionRepository
import com.voizeforms.service.TranscriptionService
import com.voizeforms.service.StreamingTranscriptionService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlin.test.*

class TranscriptionServiceTest {
    
    private lateinit var repository: TranscriptionRepository
    private lateinit var service: TranscriptionService
    
    @BeforeTest
    fun setup() {
        repository = mockk()
        service = StreamingTranscriptionService(repository)
    }
    
    @Test
    fun `should start transcription session and return session ID`() = runTest {
        // Given: A user ID
        val userId = "user-123"
        
        // When: We start a transcription session
        val sessionId = service.startTranscriptionSession(userId)
        
        // Then: We should get a valid session ID
        assertNotNull(sessionId)
        assertTrue(sessionId.isNotEmpty())
        assertTrue(sessionId.startsWith("session-"))
    }
    
    @Test
    fun `should process audio chunks and emit to hot flow`() = runTest {
        // Given: A transcription session and mock repository
        val sessionId = "test-session-456"
        val audioChunk1 = "Hello".toByteArray()
        val audioChunk2 = "World".toByteArray()
        
        coEvery { repository.addTranscriptionChunk(any(), any(), any()) } returns Unit
        
        // When: We process audio chunks
        service.processAudioChunk(sessionId, audioChunk1)
        service.processAudioChunk(sessionId, audioChunk2)
        
        // Then: Repository should be called with transcribed text
        coVerify { repository.addTranscriptionChunk(sessionId, "Hello", any()) }
        coVerify { repository.addTranscriptionChunk(sessionId, "World", any()) }
    }
    
    @Test
    fun `should subscribe to transcription stream for session`() = runTest {
        // Given: A session ID and mock repository stream
        val sessionId = "stream-session-789"
        val mockTranscriptions = listOf(
            Transcription(sessionId = sessionId, chunk = "First", confidence = 0.85),
            Transcription(sessionId = sessionId, chunk = "Second", confidence = 0.90),
            Transcription(sessionId = sessionId, chunk = "Third", confidence = 0.95)
        )
        
        every { repository.getTranscriptionStream(sessionId) } returns mockTranscriptions.asFlow()
        
        // When: We subscribe to the stream
        val collectedResults = service.subscribeToTranscriptionStream(sessionId)
            .take(3)
            .toList()
        
        // Then: We should receive transcription results
        assertEquals(3, collectedResults.size)
        assertEquals("First", collectedResults[0].text)
        assertEquals("Second", collectedResults[1].text)
        assertEquals("Third", collectedResults[2].text)
        assertTrue(collectedResults.all { it.confidence > 0.0 })
    }
    
    @Test
    fun `should handle multiple concurrent sessions`() = runTest {
        // Given: Two different session IDs
        val session1 = "concurrent-session-1"
        val session2 = "concurrent-session-2"
        val userId = "user-456"
        
        coEvery { repository.addTranscriptionChunk(any(), any(), any()) } returns Unit
        
        // When: We process audio for both sessions concurrently
        val job1 = launch {
            service.processAudioChunk(session1, "Hello from session 1".toByteArray())
        }
        
        val job2 = launch {
            service.processAudioChunk(session2, "Hello from session 2".toByteArray())
        }
        
        job1.join()
        job2.join()
        
        // Then: Both sessions should be processed independently
        coVerify { repository.addTranscriptionChunk(session1, "Hello from session 1", any()) }
        coVerify { repository.addTranscriptionChunk(session2, "Hello from session 2", any()) }
    }
    
    @Test
    fun `should end transcription session and return final result`() = runTest {
        // Given: An active session
        val sessionId = "ending-session-101"
        val finalText = "Complete transcription text"
        val savedId = "saved-transcription-123"
        
        coEvery { repository.endTranscriptionSession(sessionId, finalText) } returns savedId
        
        // When: We end the session
        val result = service.endTranscriptionSession(sessionId, finalText)
        
        // Then: Session should be ended and final result saved
        assertEquals(savedId, result)
        coVerify { repository.endTranscriptionSession(sessionId, finalText) }
    }
    
    @Test
    fun `should handle transcription errors gracefully`() = runTest {
        // Given: A session that will fail transcription
        val sessionId = "error-session-500"
        val invalidAudio = ByteArray(0) // Empty audio
        
        coEvery { repository.addTranscriptionChunk(any(), any(), any()) } returns Unit
        
        // When: We process invalid audio
        val result = runCatching {
            service.processAudioChunk(sessionId, invalidAudio)
        }
        
        // Then: Error should be handled gracefully (no exception thrown)
        assertTrue(result.isSuccess)
        
        // And: An error chunk should be added to repository
        coVerify { repository.addTranscriptionChunk(sessionId, "[Error: Unable to process audio]", 0.0) }
    }
    
    @Test
    fun `should convert repository transcriptions to service results`() = runTest {
        // Given: Repository transcription data
        val sessionId = "conversion-session-202"
        val repositoryTranscription = Transcription(
            sessionId = sessionId,
            chunk = "Test transcription",
            confidence = 0.85,
            timestamp = 1234567890L,
            isComplete = false
        )
        
        every { repository.getTranscriptionStream(sessionId) } returns flowOf(repositoryTranscription)
        
        // When: We subscribe to the stream
        val result = service.subscribeToTranscriptionStream(sessionId).first()
        
        // Then: Transcription should be converted to TranscriptionResult
        assertEquals("Test transcription", result.text)
        assertEquals(0.85, result.confidence)
        assertEquals(1234567890L, result.timestamp)
    }
    
    @Test
    fun `should maintain session state and provide session info`() = runTest {
        // Given: A started session
        val userId = "state-user-303"
        val sessionId = service.startTranscriptionSession(userId)
        
        // When: We get session info
        val sessionInfo = service.getSessionInfo(sessionId)
        
        // Then: Session info should be available
        assertNotNull(sessionInfo)
        assertEquals(sessionId, sessionInfo.sessionId)
        assertEquals(userId, sessionInfo.userId)
        assertTrue(sessionInfo.isActive)
        assertTrue(sessionInfo.startTime > 0)
    }
} 