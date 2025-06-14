package com.voizeforms.model

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionResult(
    val text: String,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
) 