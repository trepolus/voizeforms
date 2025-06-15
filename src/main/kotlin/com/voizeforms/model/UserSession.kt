package com.voizeforms.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val userId: String,
    val name: String,
    val email: String
) 