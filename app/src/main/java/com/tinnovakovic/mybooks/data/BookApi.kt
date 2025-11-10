package com.tinnovakovic.mybooks.data

import com.tinnovakovic.mybooks.data.models.BookResultApi
import com.tinnovakovic.mybooks.data.models.WorkApi
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BookApi {

    @GET("people/mekBot/books/want-to-read.json")
    fun getWantToReadBooks(
        @Query("page") page: Int
    ): Single<BookResultApi>

    @GET("{key}.json")
    fun getWork(
        @Path("key", encoded = true) key: String
    ): Single<WorkApi>

}
