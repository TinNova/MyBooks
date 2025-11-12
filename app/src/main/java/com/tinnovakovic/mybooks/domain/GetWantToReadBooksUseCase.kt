package com.tinnovakovic.mybooks.domain

import com.tinnovakovic.mybooks.data.BookRepo
import com.tinnovakovic.mybooks.domain.models.Book
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class GetWantToReadBooksUseCase @Inject constructor(
    private val repo: BookRepo,
    private val errorMapper: ErrorMapper) {

    fun execute(page: Int): Single<List<Book>> {
        return repo.getWantToReadBooks(page)
            .map { bookResultApi ->
                bookResultApi.entries.map { entry ->
                    Book(
                        title = entry.work.title,
                        key = entry.work.key,
                        authorNames = entry.work.authorNames.joinToString(", "),
                        coverUrl = COVER_IMAGE_BASE + entry.work.coverId + MEDIUM_SIZE_IMAGE
                    )
                }
            }
            .onErrorResumeNext { error ->
                Single.error(errorMapper.map(error))
            }
    }

    companion object {
        const val COVER_IMAGE_BASE = "https://covers.openlibrary.org/b/id/"
        const val MEDIUM_SIZE_IMAGE = "-M.jpg"
    }

}
