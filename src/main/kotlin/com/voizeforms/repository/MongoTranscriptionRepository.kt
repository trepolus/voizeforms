package com.voizeforms.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.voizeforms.model.Transcription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Pure MongoDB implementation of TranscriptionRepository.
 * Provides both hot flow streaming for real-time transcription chunks
 * and persistent storage using MongoDB.
 * 
 * @param database MongoDB database instance (injected dependency)
 */
class MongoTranscriptionRepository(
    private val database: MongoDatabase
) : TranscriptionRepository {
    
    private val logger = LoggerFactory.getLogger(MongoTranscriptionRepository::class.java)
    
    // MongoDB Collection
    private val transcriptionCollection: MongoCollection<Document> = 
        database.getCollection<Document>("transcriptions")
    
    // Hot Flow Management: Session-specific shared flows for real-time streaming
    private val sessionFlows = ConcurrentHashMap<String, MutableSharedFlow<Transcription>>()
    
    private val sessionMetadata = ConcurrentHashMap<String, String>()
    
    /**
     * Hot Flow: Get stream for a specific session.
     * This is a HOT FLOW - it will emit to all subscribers simultaneously.
     */
    override fun getTranscriptionStream(sessionId: String): Flow<Transcription> {
        val sharedFlow = sessionFlows.computeIfAbsent(sessionId) {
            MutableSharedFlow<Transcription>(
                replay = 0,           // Don't replay past emissions
                extraBufferCapacity = 10  // Buffer 10 items if consumers are slow
            )
        }
        
        return sharedFlow.asSharedFlow().filter { it.sessionId == sessionId }
    }
    
    /**
     * Hot Flow: Add chunk to the stream.
     * Emits immediately to all current subscribers of the session.
     */
    override suspend fun addTranscriptionChunk(sessionId: String, chunk: String, confidence: Double) {
        logger.info("Adding transcription chunk for session $sessionId: '$chunk' (confidence: $confidence)")
        
        val transcription = Transcription(
            sessionId = sessionId,
            chunk = chunk,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            isComplete = false
        )
        
        // Get or create the shared flow for this session
        val sharedFlow = sessionFlows.computeIfAbsent(sessionId) {
            logger.info("Creating new shared flow for session: $sessionId")
            MutableSharedFlow<Transcription>(
                replay = 0,
                extraBufferCapacity = 10
            )
        }
        
        // Emit to all subscribers (HOT FLOW behavior)
        try {
            sharedFlow.emit(transcription)
            logger.info("Emitted chunk to shared flow for session: $sessionId")
        } catch (e: Exception) {
            logger.error("Failed to emit chunk to shared flow for session $sessionId", e)
        }
    }
    
    /**
     * Persistence: Save complete transcription to MongoDB.
     */
    override suspend fun saveTranscription(transcription: Transcription): String {
        logger.info("Saving transcription to MongoDB: sessionId=${transcription.sessionId}, userId=${transcription.userId}, isComplete=${transcription.isComplete}")
        
        val document = Document().apply {
            append("sessionId", transcription.sessionId)
            append("chunk", transcription.chunk)
            append("confidence", transcription.confidence)
            append("timestamp", transcription.timestamp)
            append("isComplete", transcription.isComplete)
            transcription.userId?.let { 
                append("userId", it)
                logger.info("Added userId to document: $it")
            } ?: logger.warn("No userId provided for transcription - this may make it hard to retrieve later")
        }
        
        try {
            val result = transcriptionCollection.insertOne(document)
            val insertedId = result.insertedId?.asObjectId()?.value?.toHexString()
            logger.info("Successfully saved transcription with ID: $insertedId")
            return insertedId ?: throw IllegalStateException("Failed to get inserted ID")
        } catch (e: Exception) {
            logger.error("Failed to save transcription to MongoDB", e)
            throw IllegalStateException("Failed to insert transcription: ${e.message}")
        }
    }
    
    /**
     * Persistence: Retrieve transcription by session ID from MongoDB.
     */
    override suspend fun getTranscriptionBySessionId(sessionId: String): Transcription? {
        val filter = Filters.eq("sessionId", sessionId)
        val document = transcriptionCollection.find(filter)
            .sort(Sorts.descending("timestamp"))
            .limit(1)
            .toList()
            .firstOrNull()
        
        return document?.let { doc ->
            Transcription(
                id = doc.getObjectId("_id")?.toHexString(),
                sessionId = doc.getString("sessionId"),
                chunk = doc.getString("chunk"),
                confidence = doc.getDouble("confidence") ?: 0.0,
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                isComplete = doc.getBoolean("isComplete") ?: false,
                userId = doc.getString("userId")
            )
        }
    }
    
    /**
     * Persistence: Retrieve transcriptions by user ID from MongoDB.
     */
    override suspend fun getTranscriptionsByUserId(userId: String): List<Transcription> {
        logger.info("Querying transcriptions for userId: $userId")
        
        val filter = Filters.eq("userId", userId)
        val documents = transcriptionCollection.find(filter)
            .sort(Sorts.descending("timestamp"))
            .toList()
        
        logger.info("Found ${documents.size} documents for userId: $userId")
        
        val transcriptions = documents.map { doc ->
            Transcription(
                id = doc.getObjectId("_id")?.toHexString(),
                sessionId = doc.getString("sessionId"),
                chunk = doc.getString("chunk"),
                confidence = doc.getDouble("confidence") ?: 0.0,
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                isComplete = doc.getBoolean("isComplete") ?: false,
                userId = doc.getString("userId")
            )
        }
        
        logger.info("Returning ${transcriptions.size} transcriptions for userId: $userId")
        return transcriptions
    }
    
    /**
     * Session Management: End transcription session and clean up.
     */
    override suspend fun endTranscriptionSession(sessionId: String, finalText: String?): String? {
        logger.info("Ending transcription session: $sessionId with finalText: $finalText")
        
        val userId = sessionMetadata[sessionId]
        logger.info("Retrieved userId for session $sessionId: $userId")
        
        sessionFlows.remove(sessionId)
        sessionMetadata.remove(sessionId)
        logger.info("Cleaned up session flows and metadata for: $sessionId")
        
        // Optionally save the final complete transcription to MongoDB
        return finalText?.let { text ->
            logger.info("Creating final transcription for session $sessionId with userId: $userId")
            val finalTranscription = Transcription(
                sessionId = sessionId,
                chunk = text,
                confidence = 1.0,
                isComplete = true,
                userId = userId
            )
            val savedId = saveTranscription(finalTranscription)
            logger.info("Saved final transcription with ID: $savedId")
            savedId
        } ?: run {
            logger.warn("No final text provided for session $sessionId - not saving to database")
            "unknown"
        }
    }
    
    fun registerSession(sessionId: String, userId: String) {
        logger.info("Registering session $sessionId for user: $userId")
        sessionMetadata[sessionId] = userId
    }
} 