package com.formulaknowledge.app.data

data class NewsArticleResponse(
    val id: Int,
    val title: String,
    val source: String,
    val url: String,
    val image_url: String?,
    val published_at: String
)