package com.tinnovakovic.mybooks.data.models

import com.google.gson.annotations.SerializedName

data class WorkApi(
    val title: String?,
    @SerializedName("subject_places")
    val subjectPlaces: List<String>?,
    @SerializedName("first_publish_date")
    val firstPublishDate: String?,
    val subject: List<String>?,
    val description: Description?,
    @SerializedName("latest_revision")
    val latestRevision: Int?,
)

data class Description(
    val type: String,
    val value: String,
)
