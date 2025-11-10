package com.tinnovakovic.mybooks.data.models

import com.google.gson.annotations.SerializedName

data class BookResultApi(
    val page: Int,
    val numFound: Int,
    @SerializedName("reading_log_entries")
    val entries: List<Entry>
)

data class Entry(
    val work: Work
)

data class Work(
    val title: String,
    val key: String,
    @SerializedName("author_names")
    val authorNames: List<String>,
    @SerializedName("cover_id")
    val coverId: String,
)
