package com.voizeforms.integration.repository

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.voizeforms.model.Transcription
import com.voizeforms.repository.MongoTranscriptionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.*

/**
 * Integration tests for MongoTranscriptionRepository using TestContainers.
 * These tests verify that the repository works correctly with a real MongoDB instance.
 *
 * To run these tests: ./gradlew test --tests "*Integration*"
 * Requires Docker to be running.
 */
@Testcontainers
@Ignore // Disable by default - requires Docker
class MongoTranscriptionRepositoryIntegrationTest {

    companion object {
        @Container
        val mongoContainer = MongoDBContainer("mongo:7.0").apply {
            withExposedPorts(27017)
        }
    }

    private lateinit var repository: MongoTranscriptionRepository
    private lateinit var database: MongoDatabase

    @BeforeTest
    fun setup() {
        // Connect to TestContainer MongoDB instance
        val client = MongoClient.create(mongoContainer.connectionString)
        database = client.getDatabase("test_voizeforms")
        repository = MongoTranscriptionRepository(database)
    }

    @AfterTest
    fun cleanup() {
        // Clean up test data
        runTest {
            database.drop()
        }
    }

    @Test
    fun `should save and retrieve transcription from real MongoDB`() = runTest {
        // Given: A transcription to save
        val transcription = Transcription(
            sessionId = "integration-session-123",
            chunk = "Hello from MongoDB integration test",
            confidence = 0.95,
            isComplete = true,
            userId = "test-user-456"
        )

        // When: We save it to MongoDB
        val savedId = repository.saveTranscription(transcription)

        // And: We retrieve it by session ID
        val retrieved = repository.getTranscriptionBySessionId("integration-session-123")

        // Then: We should get the same transcription data
        assertNotNull(retrieved)
        assertEquals(savedId, retrieved.id)
        assertEquals(transcription.sessionId, retrieved.sessionId)
        assertEquals(transcription.chunk, retrieved.chunk)
        assertEquals(transcription.confidence, retrieved.confidence)
        assertEquals(transcription.isComplete, retrieved.isComplete)
        assertEquals(transcription.userId, retrieved.userId)
    }

    @Test
    fun `should retrieve transcriptions by user ID from MongoDB`() = runTest {
        // Given: Multiple transcriptions for the same user
        val userId = "mongodb-user-789"
        val transcriptions = listOf(
            Transcription(
                sessionId = "session-1",
                chunk = "First transcription",
                userId = userId,
                timestamp = 1000L
            ),
            Transcription(
                sessionId = "session-2",
                chunk = "Second transcription",
                userId = userId,
                timestamp = 2000L
            ),
            Transcription(
                sessionId = "session-3",
                chunk = "Different user",
                userId = "different-user"
            )
        )

        // When: We save all transcriptions
        transcriptions.forEach { repository.saveTranscription(it) }

        // And: We retrieve by user ID
        val userTranscriptions = repository.getTranscriptionsByUserId(userId)

        // Then: We should get only the transcriptions for that user, ordered by timestamp (newest first)
        assertEquals(2, userTranscriptions.size)
        assertEquals("Second transcription", userTranscriptions[0].chunk) // Newest first
        assertEquals("First transcription", userTranscriptions[1].chunk)
        assertTrue(userTranscriptions.all { it.userId == userId })
    }

    @Test
    fun `should support hot flow with real MongoDB persistence`() = runTest {
        // Given: A session ID for streaming
        val sessionId = "mongodb-hotflow-session"

        // When: We subscribe to the hot flow
        val collectedChunks = mutableListOf<Transcription>()
        val job = launch {
            repository.getTranscriptionStream(sessionId)
                .take(3) // Take first 3 emissions
                .toList()
                .forEach { collectedChunks.add(it) }
        }

        // And: We add chunks to the stream (these won't be persisted automatically)
        delay(100) // Give subscription time to start
        repository.addTranscriptionChunk(sessionId, "MongoDB chunk 1", 0.9)
        repository.addTranscriptionChunk(sessionId, "MongoDB chunk 2", 0.8)
        repository.addTranscriptionChunk(sessionId, "MongoDB chunk 3", 0.7)

        // Wait for collection to complete
        job.join()

        // Then: All chunks should be collected from the hot flow
        assertEquals(3, collectedChunks.size)
        assertEquals("MongoDB chunk 1", collectedChunks[0].chunk)
        assertEquals("MongoDB chunk 2", collectedChunks[1].chunk)
        assertEquals("MongoDB chunk 3", collectedChunks[2].chunk)
        assertEquals(0.9, collectedChunks[0].confidence)
        assertEquals(0.8, collectedChunks[1].confidence)
        assertEquals(0.7, collectedChunks[2].confidence)
        assertTrue(collectedChunks.all { it.sessionId == sessionId })
    }

    @Test
    fun `should end session and persist final transcription to MongoDB`() = runTest {
        // Given: A session with some activity
        val sessionId = "mongodb-end-session-test"

        // When: We add some chunks to the hot flow
        repository.addTranscriptionChunk(sessionId, "Chunk 1", 0.9)
        repository.addTranscriptionChunk(sessionId, "Chunk 2", 0.8)

        // And: We end the session with a final transcription
        val finalText = "Complete final transcription for MongoDB test"
        val finalId = repository.endTranscriptionSession(sessionId, finalText)

        // Then: The final transcription should be saved to MongoDB
        assertNotNull(finalId)

        val savedTranscription = repository.getTranscriptionBySessionId(sessionId)
        assertNotNull(savedTranscription)
        assertEquals(finalText, savedTranscription.chunk)
        assertEquals(1.0, savedTranscription.confidence)
        assertTrue(savedTranscription.isComplete)
        assertEquals(finalId, savedTranscription.id)
    }

    @Test
    fun `should handle MongoDB connection errors gracefully`() = runTest {
        // Given: A transcription to save
        val transcription = Transcription(
            sessionId = "error-handling-test",
            chunk = "Test transcription",
            confidence = 0.85
        )

        // When: We save and retrieve (should not throw exceptions)
        val id = repository.saveTranscription(transcription)
        assertNotNull(id)

        val retrieved = repository.getTranscriptionBySessionId("error-handling-test")
        assertNotNull(retrieved)
        assertEquals("Test transcription", retrieved.chunk)
    }
} 