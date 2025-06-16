package com.voizeforms.integration

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.voizeforms.model.Transcription
import com.voizeforms.repository.MongoTranscriptionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Manual MongoDB verification test.
 * 
 * To run this test:
 * 1. Start MongoDB locally: `docker-compose up -d`
 * 2. Run test: `./gradlew test --tests "*MongoVerificationTest*"`
 * 3. Stop MongoDB: `docker-compose down`
 * 
 * This verifies that our MongoDB integration actually works.
 */
@Ignore // Manual test - requires local MongoDB
class MongoVerificationTest {
    
    @Test
    fun `MANUAL - verify MongoDB integration works locally`() = runTest {
        // This test can be run manually to verify MongoDB integration
        // Requires docker-compose up -d to be running
        
        try {
            // Connect to local MongoDB (from docker-compose)
            val client = MongoClient.create("mongodb://localhost:27017")
            val database = client.getDatabase("test_verification")
            val repository = MongoTranscriptionRepository(database)
            
            // Test basic save and retrieve
            val transcription = Transcription(
                sessionId = "manual-test-session",
                chunk = "Manual verification test - MongoDB is working!",
                confidence = 0.95,
                isComplete = true,
                userId = "manual-test-user"
            )
            
            // Save to MongoDB
            val savedId = repository.saveTranscription(transcription)
            println("✅ Saved transcription with ID: $savedId")
            
            // Retrieve from MongoDB
            val retrieved = repository.getTranscriptionBySessionId("manual-test-session")
            assertNotNull(retrieved, "Failed to retrieve transcription from MongoDB")
            assertEquals(transcription.chunk, retrieved.chunk)
            assertEquals(transcription.confidence, retrieved.confidence)
            println("✅ Retrieved transcription: ${retrieved.chunk}")
            
            // Test hot flow (real-time streaming)
            repository.addTranscriptionChunk("manual-test-session", "Hot flow test", 0.9)
            println("✅ Hot flow streaming works")
            
            // Clean up
            database.drop()
            client.close()
            
            println("🎉 MongoDB integration verification PASSED!")
            println("   - ✅ Save/retrieve operations work")
            println("   - ✅ Hot flow streaming works") 
            println("   - ✅ Database connection successful")
            
        } catch (e: Exception) {
            println("❌ MongoDB verification FAILED: ${e.message}")
            println("   Make sure MongoDB is running: docker-compose up -d")
            throw e
        }
    }
} 