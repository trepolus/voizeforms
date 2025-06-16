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
    
    // MongoDB Collection
    private val transcriptionCollection: MongoCollection<Document> = 
        database.getCollection<Document>("transcriptions")
    
    // Hot Flow Management: Session-specific shared flows for real-time streaming
    private val sessionFlows = ConcurrentHashMap<String, MutableSharedFlow<Transcription>>()
    
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
        val transcription = Transcription(
            sessionId = sessionId,
            chunk = chunk,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            isComplete = false
        )
        
        // Get or create the shared flow for this session
        val sharedFlow = sessionFlows.computeIfAbsent(sessionId) {
            MutableSharedFlow<Transcription>(
                replay = 0,
                extraBufferCapacity = 10
            )
        }
        
        // Emit to all subscribers (HOT FLOW behavior)
        sharedFlow.emit(transcription)
    }
    
    /**
     * Persistence: Save complete transcription to MongoDB.
     */
    override suspend fun saveTranscription(transcription: Transcription): String {
        val document = Document().apply {
            append("sessionId", transcription.sessionId)
            append("chunk", transcription.chunk)
            append("confidence", transcription.confidence)
            append("timestamp", transcription.timestamp)
            append("isComplete", transcription.isComplete)
            transcription.userId?.let { append("userId", it) }
        }
        
        val result = transcriptionCollection.insertOne(document)
        return result.insertedId?.asObjectId()?.value?.toHexString() 
            ?: throw IllegalStateException("Failed to insert transcription")
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
        val filter = Filters.eq("userId", userId)
        val documents = transcriptionCollection.find(filter)
            .sort(Sorts.descending("timestamp"))
            .toList()
        
        return documents.map { doc ->
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
     * Session Management: End transcription session and clean up.
     */
    override suspend fun endTranscriptionSession(sessionId: String, finalText: String?): String? {
        // Remove the hot flow for this session (clean up memory)
        sessionFlows.remove(sessionId)
        
        // Optionally save the final complete transcription to MongoDB
        return finalText?.let { text ->
            val finalTranscription = Transcription(
                sessionId = sessionId,
                chunk = text,
                confidence = 1.0,
                isComplete = true
            )
            saveTranscription(finalTranscription)
        }
    }
} 