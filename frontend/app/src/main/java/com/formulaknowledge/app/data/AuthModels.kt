package com.formulaknowledge.app.data

data class UserLoginRequest(val email: String, val password: String)

data class UserRegisterRequest(val email: String, val password: String, val full_name: String? = null)

data class TokenResponse(val access_token: String, val token_type: String)