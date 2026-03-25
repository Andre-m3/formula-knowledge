package com.formulaknowledge.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Indirizzo magico per far parlare l'emulatore Android col tuo computer
    private const val BASE_URL = "http://10.0.2.2:8000"

    val apiService: F1ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Converte il JSON in Kotlin
            .build()
            .create(F1ApiService::class.java)
    }
}