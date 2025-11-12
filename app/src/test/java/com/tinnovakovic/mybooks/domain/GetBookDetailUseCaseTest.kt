package com.tinnovakovic.mybooks.domain

import com.tinnovakovic.mybooks.data.BookRepo
import com.tinnovakovic.mybooks.data.models.WorkApi
import com.tinnovakovic.mybooks.domain.models.BookDetail
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class GetBookDetailUseCaseTest {

    private var repo: BookRepo = mockk()
    private var errorMapper: ErrorMapper = mockk()
    private lateinit var sut: GetBookDetailUseCase

    @BeforeEach
    fun setup() {
        sut = GetBookDetailUseCase(repo, errorMapper)
    }

    @Test
    fun `WHEN execute() THEN returns BookDetail`() {
        // Given
        val key = "key"
        val workApi = WorkApi(
            title = "Test Book",
            subjectPlaces = listOf("New York", "London"),
            firstPublishDate = "2020-01-01",
            subject = listOf("Fiction", "Adventure"),
            _description = "A great adventure story",
            latestRevision = 5
        )
        val expectedBookDetail = BookDetail(
            title = "Test Book",
            firstPublishDate = "2020-01-01",
            latestRevision = "5",
            description = "A great adventure story",
            subjectPlaces = listOf("New York", "London")
        )
        every { repo.getWork(key) } returns Single.just(workApi)

        // When
        val result = sut.execute(key).test()

        // Then
        result.assertValue(expectedBookDetail)
        verify { repo.getWork(key) }
    }

    @Test
    fun `GIVEN null WorkApi values, WHEN execute(), THEN returns BookDetail`() {
        // Given
        val key = "key"
        val workApi = WorkApi(
            title = null,
            subjectPlaces = null,
            firstPublishDate = null,
            subject = null,
            _description = null,
            latestRevision = null
        )
        val expectedBookDetail = BookDetail(
            title = "",
            firstPublishDate = "",
            latestRevision = "",
            description = "",
            subjectPlaces = emptyList()
        )
        every { repo.getWork(key) } returns Single.just(workApi)

        // When
        val result = sut.execute(key).test()

        // Then
        result.assertNoErrors()
        result.assertValue(expectedBookDetail)
        verify { repo.getWork(key) }
    }

    @Test
    fun `GIVEN exception, WHEN execute(), THEN map and return exception`() {
        // Given
        val key = "key"
        val ioException = IOException("Connection timeout")
        val mappedException = ErrorMapper.NetworkException.NoConnection("Network connection failed", ioException)
        every { repo.getWork(key) } returns Single.error(ioException)
        every { errorMapper.map(ioException) } returns mappedException

        // When
        val result = sut.execute(key).test()

        // Then
        result.assertNotComplete()
        result.assertError(mappedException)
        verify { repo.getWork(key) }
    }
}
