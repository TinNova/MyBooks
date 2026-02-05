package com.tinnovakovic.mybooks.data

import com.tinnovakovic.mybooks.ContextWrapper
import com.tinnovakovic.mybooks.data.models.BookResultApi
import com.tinnovakovic.mybooks.data.models.WorkApi
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class BookRepo @Inject constructor(
    private val bookApi: BookApi,
    private val contextWrapper: ContextWrapper) {

    fun getWantToReadBooks(page: Int): Single<BookResultApi> {
        if (contextWrapper.getContext() != null) {
            println("TINTIN Context is here")
        }
        return bookApi.getWantToReadBooks(page = page)
    }

    fun getWork(key: String): Single<WorkApi> {
        return bookApi.getWork(key = key)
    }
}
