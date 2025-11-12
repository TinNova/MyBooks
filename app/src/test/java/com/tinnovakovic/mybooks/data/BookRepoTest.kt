package com.tinnovakovic.mybooks.data

import com.tinnovakovic.mybooks.data.models.BookResultApi
import com.tinnovakovic.mybooks.data.models.Entry
import com.tinnovakovic.mybooks.data.models.Work
import com.tinnovakovic.mybooks.data.models.WorkApi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BookRepoTest {

    private var bookApi: BookApi = mockk()
    private lateinit var sut: BookRepo

    @BeforeEach
    fun setup() {
        sut = BookRepo(bookApi)
    }

    @Test
    fun `GIVEN page, WHEN getWantToReadBooks(), THEN takes the same page and returns success`() {
        // Given
        val page = 1
        val expectedResult = BookResultApi(
            page = 1,
            numFound = 2,
            entries = listOf(
                Entry(
                    work = Work(
                        title = "Test Book 1",
                        key = "/works/OL123W",
                        authorNames = listOf("Author 1"),
                        coverId = "123"
                    )
                ),
                Entry(
                    work = Work(
                        title = "Test Book 2",
                        key = "/works/OL456W",
                        authorNames = listOf("Author 2"),
                        coverId = "456"
                    )
                )
            )
        )
        every { bookApi.getWantToReadBooks(page) } returns Single.just(expectedResult)

        // When
        val result = sut.getWantToReadBooks(page).test()

        // Then
        result.assertNoErrors()
        result.assertValue(expectedResult)
        verify { bookApi.getWantToReadBooks(page) }

    }

    @Test
    fun `GIVEN Exception, WHEN getWantToReadBooks() THEN propagates error`() {
        // Given
        val page = 1
        val expectedException = RuntimeException("Network error")
        every { bookApi.getWantToReadBooks(page) } returns Single.error(expectedException)

        // When
        val result = sut.getWantToReadBooks(page).test()

        // Then
        result.assertNotComplete()
        result.assertError(expectedException)
        verify { bookApi.getWantToReadBooks(page) }
    }

    @Test
    fun `GIVEN key, WHEN getWork(), THEN it takes the same key and returns successfully`() {
        // Given
        val key = "key"
        val expectedWork = WorkApi(
            title = "Title",
            subjectPlaces = listOf("New York", "London"),
            firstPublishDate = "2020-01-01",
            subject = listOf("Fiction", "Adventure"),
            _description = "A great book about adventure",
            latestRevision = 5
        )
        every { bookApi.getWork(key) } returns Single.just(expectedWork)

        // When
        val result = sut.getWork(key).test()

        // Then
        result.assertValue(expectedWork)
        verify { bookApi.getWork(key) }

    }

    @Test
    fun `GIVEN Exception, WHEN getWork() THEN propagates error`() {
        // Given
        val key = "key"
        val expectedException = RuntimeException("Network error")
        every { bookApi.getWork(key) } returns Single.error(expectedException)

        // When
        val result = sut.getWork(key).test()

        // Then
        result.assertNotComplete()
        result.assertError(expectedException)
    }
}
