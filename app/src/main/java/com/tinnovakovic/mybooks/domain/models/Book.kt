package com.tinnovakovic.mybooks.domain.models

data class Book(
    val title: String,
    val key: String,
    val authorNames: String,
    val coverUrl: String,
)