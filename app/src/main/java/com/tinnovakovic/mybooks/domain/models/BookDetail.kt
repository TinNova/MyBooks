package com.tinnovakovic.mybooks.domain.models

data class BookDetail(
    val title: String,
    val firstPublishDate: String,
    val latestRevision: String,
    val description: String,
    val subjectPlaces: List<String>,
)