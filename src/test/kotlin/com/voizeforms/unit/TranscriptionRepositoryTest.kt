package com.voizeforms.unit

import com.voizeforms.model.Transcription
import com.voizeforms.repository.TranscriptionRepository
import com.voizeforms.repository.InMemoryTranscriptionRepository
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.test.*

class TranscriptionRepositoryTest {
    
    private lateinit var repository: TranscriptionRepository
    
    @BeforeTest
    fun setup() {
        // Use pure in-memory implementation for fast unit testing
        repository = InMemoryTranscriptionRepository()
    }
    
    @Test
    fun `should emit transcription chunks as hot flow`() = runTest {
        // Given: A session ID
        val sessionId = "test-session-123"
        
        // When: We subscribe to the hot flow
        val collectedChunks = mutableListOf<Transcription>()
        val job = launch {
            repository.getTranscriptionStream(sessionId)
                .take(3) // Take first 3 emissions
                .toList()
                .forEach { collectedChunks.add(it) }
        }
        
        // And: We add chunks to the stream
        delay(100) // Give subscription time to start
        repository.addTranscriptionChunk(sessionId, "Hello")
        repository.addTranscriptionChunk(sessionId, "World")
        repository.addTranscriptionChunk(sessionId, "!")
        
        // Wait for collection to complete
        job.join()
        
        // Then: All chunks should be collected
        assertEquals(3, collectedChunks.size)
        assertEquals("Hello", collectedChunks[0].chunk)
        assertEquals("World", collectedChunks[1].chunk)
        assertEquals("!", collectedChunks[2].chunk)
        assertTrue(collectedChunks.all { it.sessionId == sessionId })
    }
    
    @Test
    fun `should support multiple subscribers to same session`() = runTest {
        // Given: A session ID and two subscribers
        val sessionId = "multi-session-456"
        val subscriber1Chunks = mutableListOf<Transcription>()
        val subscriber2Chunks = mutableListOf<Transcription>()
        
        // When: Both subscribe to the same session
        val job1 = launch {
            repository.getTranscriptionStream(sessionId)
                .take(2)
                .toList()
                .forEach { subscriber1Chunks.add(it) }
        }
        
        val job2 = launch {
            repository.getTranscriptionStream(sessionId)
                .take(2)
                .toList()
                .forEach { subscriber2Chunks.add(it) }
        }
        
        delay(100) // Let subscriptions start
        
        // And: We add chunks
        repository.addTranscriptionChunk(sessionId, "Shared")
        repository.addTranscriptionChunk(sessionId, "Data")
        
        // Wait for both to complete
        job1.join()
        job2.join()
        
        // Then: Both subscribers should receive the same data
        assertEquals(2, subscriber1Chunks.size)
        assertEquals(2, subscriber2Chunks.size)
        assertEquals(subscriber1Chunks[0].chunk, subscriber2Chunks[0].chunk)
        assertEquals(subscriber1Chunks[1].chunk, subscriber2Chunks[1].chunk)
    }
    
    @Test
    fun `should persist complete transcription to database`() = runTest {
        // Given: A complete transcription
        val transcription = Transcription(
            sessionId = "persist-session-789",
            chunk = "Complete transcription text",
            isComplete = true,
            confidence = 0.95
        )
        
        // When: We save it
        val savedId = repository.saveTranscription(transcription)
        
        // Then: It should be persisted with an ID
        assertNotNull(savedId)
        assertTrue(savedId.isNotEmpty())
    }
    
    @Test
    fun `should retrieve transcription by session ID`() = runTest {
        // Given: A saved transcription
        val originalTranscription = Transcription(
            sessionId = "retrieve-session-101",
            chunk = "Retrievable text",
            isComplete = true,
            confidence = 0.88
        )
        
        val savedId = repository.saveTranscription(originalTranscription)
        
        // When: We retrieve by session ID
        val retrieved = repository.getTranscriptionBySessionId("retrieve-session-101")
        
        // Then: We should get the same transcription
        assertNotNull(retrieved)
        assertEquals(originalTranscription.sessionId, retrieved.sessionId)
        assertEquals(originalTranscription.chunk, retrieved.chunk)
        assertEquals(originalTranscription.confidence, retrieved.confidence)
        assertEquals(savedId, retrieved.id)
    }
} 