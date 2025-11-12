package com.tinnovakovic.mybooks.domain

import com.tinnovakovic.mybooks.data.BookRepo
import com.tinnovakovic.mybooks.data.models.BookResultApi
import com.tinnovakovic.mybooks.data.models.Entry
import com.tinnovakovic.mybooks.data.models.Work
import com.tinnovakovic.mybooks.domain.models.Book
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class GetWantToReadBooksUseCaseTest {

    private lateinit var repo: BookRepo
    private lateinit var errorMapper: ErrorMapper
    private lateinit var sut: GetWantToReadBooksUseCase

    @BeforeEach
    fun setup() {
        repo = mockk()
        errorMapper = mockk()
        sut = GetWantToReadBooksUseCase(repo, errorMapper)
    }

    @Test
    fun `WHEN execute(), THEN return list of Books successfully`() {
        // Given
        val page = 1
        val bookResultApi = BookResultApi(
            page = 1,
            numFound = 2,
            entries = listOf(
                Entry(
                    work = Work(
                        title = "Book One",
                        key = "/works/OL123W",
                        authorNames = listOf("Author One", "Author Two"),
                        coverId = "12345"
                    )
                ),
                Entry(
                    work = Work(
                        title = "Book Two",
                        key = "/works/OL456W",
                        authorNames = listOf("Author Three"),
                        coverId = "67890"
                    )
                )
            )
        )
        val expectedBooks = listOf(
            Book(
                title = "Book One",
                key = "/works/OL123W",
                authorNames = "Author One, Author Two",
                coverUrl = "https://covers.openlibrary.org/b/id/12345-M.jpg"
            ),
            Book(
                title = "Book Two",
                key = "/works/OL456W",
                authorNames = "Author Three",
                coverUrl = "https://covers.openlibrary.org/b/id/67890-M.jpg"
            )
        )
        every { repo.getWantToReadBooks(page) } returns Single.just(bookResultApi)

        // When
        val result = sut.execute(page).test()

        // Then
        result.assertValue(expectedBooks)
        verify { repo.getWantToReadBooks(page) }
    }

    @Test
    fun `GIVEN no entries, WHEN execute(), THEN return empty list of Books`() {
        // Given
        val page = 1
        val bookResultApi = BookResultApi(
            page = 1,
            numFound = 0,
            entries = emptyList()
        )
        every { repo.getWantToReadBooks(page) } returns Single.just(bookResultApi)

        // When
        val result = sut.execute(page).test()

        // Then
        result.assertValue(emptyList())
        verify { repo.getWantToReadBooks(page) }
    }

    @Test
    fun `GIVEN exception, WHEN execute(), THEN map and return error`() {
        // Given
        val page = 1
        val ioException = IOException("Network timeout")
        val mappedException = ErrorMapper.NetworkException.NoConnection("Network connection failed", ioException)
        every { repo.getWantToReadBooks(page) } returns Single.error(ioException)
        every { errorMapper.map(ioException) } returns mappedException

        // When
        val result = sut.execute(page).test()

        // Then
        result.assertError(mappedException)
        verify { repo.getWantToReadBooks(page) }
        verify { errorMapper.map(ioException) }
    }

}
