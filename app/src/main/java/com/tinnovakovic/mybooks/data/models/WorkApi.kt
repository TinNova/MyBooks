package com.tinnovakovic.mybooks.data.models

import com.google.gson.annotations.SerializedName

data class WorkApi(
    val title: String?,
    @SerializedName("subject_places")
    val subjectPlaces: List<String>?,
    @SerializedName("first_publish_date")
    val firstPublishDate: String?,
    val subject: List<String>?,
    @SerializedName("description")
    private val _description: Any?,
    @SerializedName("latest_revision")
    val latestRevision: Int?,
) {
    val description: String?
        get() = when (_description) {
            is String -> _description
            is Map<*, *> -> (_description["value"] as? String)
            else -> null
        }
}
