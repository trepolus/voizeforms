package com.voizeforms.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorInfo(
    val code: String,
    val message: String,
    val details: Map<String, String?> = emptyMap()
)

@Serializable
data class ErrorResponse(
    val error: ErrorInfo
) 