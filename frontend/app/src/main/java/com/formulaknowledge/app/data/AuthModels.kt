package com.formulaknowledge.app.data

data class UserLoginRequest(val email: String, val password: String)

data class UserRegisterRequest(val email: String, val password: String, val full_name: String? = null)

data class TokenResponse(val access_token: String, val token_type: String)

data class UserProfileResponse(
    val id: Int,
    val email: String,
    val full_name: String?,
    val favorite_constructor_id: String?,
    val auth_provider: String
)