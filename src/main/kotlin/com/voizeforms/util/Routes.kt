package com.voizeforms.util

object Routes {
    const val API_V1 = "/api/v1"

    // Transcription
    const val TRANSCRIBE = "$API_V1/transcribe"
    const val TRANSCRIPTION_STREAM = "$API_V1/transcription/stream"
    const val TRANSCRIPTION_SESSION = "$API_V1/transcription/session"
    const val TRANSCRIPTION_HISTORY = "$API_V1/transcription/history"
} 