package com.voizeforms.model

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Transcription(
    val id: String? = null,
    val sessionId: String,
    val chunk: String,
    val confidence: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isComplete: Boolean = false,
    val userId: String? = null
) {
    // Constructor for MongoDB ObjectId conversion
    constructor(
        objectId: ObjectId?,
        sessionId: String,
        chunk: String,
        confidence: Double = 0.0,
        timestamp: Long = System.currentTimeMillis(),
        isComplete: Boolean = false,
        userId: String? = null
    ) : this(
        id = objectId?.toHexString(),
        sessionId = sessionId,
        chunk = chunk,
        confidence = confidence,
        timestamp = timestamp,
        isComplete = isComplete,
        userId = userId
    )
} 