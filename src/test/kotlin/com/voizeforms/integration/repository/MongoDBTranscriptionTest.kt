package com.voizeforms.integration.repository

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.voizeforms.model.Transcription
import com.voizeforms.repository.MongoTranscriptionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@Ignore // Uncomment @Ignore to run manually when docker is running
class MongoDBTranscriptionTest {

    private lateinit var repository: MongoTranscriptionRepository
    private lateinit var database: MongoDatabase
    private lateinit var mongoClient: MongoClient

    @BeforeTest
    fun setup() {
        // Connect to docker MongoDB instance
        mongoClient = MongoClient.create("mongodb://localhost:27017")
        database = mongoClient.getDatabase("test_voizeforms_compose")
        repository = MongoTranscriptionRepository(database)
    }

    @AfterTest
    fun cleanup() {
        runTest {
            database.drop()
        }
        mongoClient.close()
    }

    @Test
    fun `should save and retrieve transcription from DB`() = runTest {
        // Given: A transcription to save
        val transcription = Transcription(
            sessionId = "compose-session-123",
            chunk = "Hello from docker-compose MongoDB test",
            confidence = 0.95,
            isComplete = true,
            userId = "test-user-456"
        )

        // When: We save it to MongoDB
        val savedId = repository.saveTranscription(transcription)

        // And: We retrieve it by session ID
        val retrieved = repository.getTranscriptionBySessionId("compose-session-123")

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
    fun `should verify hot flow works with DB`() = runTest {
        // Given: A session ID
        val sessionId = "compose-hotflow-test"

        // When: We perform repository operations
        repository.addTranscriptionChunk(sessionId, "Docker compose chunk", 0.8)

        val finalTranscription = Transcription(
            sessionId = sessionId,
            chunk = "Final transcription via docker-compose",
            confidence = 0.9,
            isComplete = true
        )

        val savedId = repository.saveTranscription(finalTranscription)

        // Then: All operations should work correctly
        assertNotNull(savedId)

        val retrieved = repository.getTranscriptionBySessionId(sessionId)
        assertNotNull(retrieved)
        assertEquals("Final transcription via docker-compose", retrieved.chunk)
        assertTrue(retrieved.isComplete)
    }
} 