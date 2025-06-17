package com.voizeforms.util

object Routes {
    const val API_V1 = "/api/v1"

    // Cold transcription endpoint
    const val TRANSCRIBE = "$API_V1/transcribe"

    // Streaming / session-related
    const val TRANSCRIPTION_BASE = "$API_V1/transcription"
    const val TRANSCRIPTION_STREAM = "$TRANSCRIPTION_BASE/stream"
} 