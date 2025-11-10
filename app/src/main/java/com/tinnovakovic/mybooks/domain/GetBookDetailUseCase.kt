package com.tinnovakovic.mybooks.domain

import com.tinnovakovic.mybooks.data.BookRepo
import com.tinnovakovic.mybooks.domain.models.BookDetail
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class GetBookDetailUseCase @Inject constructor(private val repo: BookRepo) {

    fun execute(key: String): Single<BookDetail> {
        return repo.getWork(key)
            .map { workApi ->
                BookDetail(
                    title = workApi.title?: "",
                    firstPublishDate = workApi.firstPublishDate ?: "",
                    latestRevision = workApi.latestRevision.toString(),
                    description = workApi.description?.value ?: "",
                    subjectPlaces = workApi.subjectPlaces ?: emptyList()
                )
            }
    }
}
