package com.voizeforms.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionInfo(
    val sessionId: String,
    val userId: String,
    val isActive: Boolean,
    val startTime: Long,
    val endTime: Long? = null,
    val totalChunks: Int = 0
) 