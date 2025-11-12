package com.tinnovakovic.mybooks.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.tinnovakovic.mybooks.domain.GetBookDetailUseCase
import com.tinnovakovic.mybooks.domain.GetWantToReadBooksUseCase
import com.tinnovakovic.mybooks.domain.models.Book
import com.tinnovakovic.mybooks.domain.models.BookDetail
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(InstantTaskExecutorExtension::class)
class BookViewModelTest {

    // Test rule for LiveData to execute synchronously
    private val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var getWantToReadBooksUseCase: GetWantToReadBooksUseCase
    private lateinit var getBookDetailUseCase: GetBookDetailUseCase
    private lateinit var sut: BookViewModel

    @BeforeEach
    fun setup() {
        // Set RxJava schedulers to run synchronously for tests
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        getWantToReadBooksUseCase = mockk()
        getBookDetailUseCase = mockk()
        sut = BookViewModel(getWantToReadBooksUseCase, getBookDetailUseCase)
    }

    @AfterEach
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Nested
    inner class InitialiseEvent {

        @Test
        fun `GIVEN successful response, WHEN Initialise event, THEN update booksLiveData and isLoadingLiveData`() {
        // Given
        val expectedBooks = listOf(
            Book(
                title = "Book One",
                key = "key",
                authorNames = "Author One",
                coverUrl = "url1"
            ),
            Book(
                title = "Book Two",
                key = "key",
                authorNames = "Author Two",
                coverUrl = "url2"
            )
        )
        every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(expectedBooks)

        val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)
        val loadingObserver = mockk<Observer<Boolean>>(relaxed = true)
        val errorObserver = mockk<Observer<BookContract.Error?>>(relaxed = true)

        sut.booksLiveData.observeForever(booksObserver)
        sut.isLoadingLiveData.observeForever(loadingObserver)
        sut.errorLiveData.observeForever(errorObserver)

        // When
        sut.onUiEvent(BookContract.UiEvent.Initialise)

        // Then
        verify { getWantToReadBooksUseCase.execute(page = 1) }
        verify { booksObserver.onChanged(emptyList()) } // Initial value
        verify { booksObserver.onChanged(expectedBooks) } // Final value
        verify { loadingObserver.onChanged(false) } // Initial value
        verify { loadingObserver.onChanged(true) } // Loading started
        verify { loadingObserver.onChanged(false) } // Loading finished
        verify { errorObserver.onChanged(null) } // Initial and cleared error

        // Cleanup
        sut.booksLiveData.removeObserver(booksObserver)
        sut.isLoadingLiveData.removeObserver(loadingObserver)
        sut.errorLiveData.removeObserver(errorObserver)
    }

        @Test
        fun `GIVEN empty response, WHEN Initialise event, THEN update booksLiveData with empty list and cannot load more books`() {
            // Given
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(emptyList())

            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)
            val loadingObserver = mockk<Observer<Boolean>>(relaxed = true)

            sut.booksLiveData.observeForever(booksObserver)
            sut.isLoadingLiveData.observeForever(loadingObserver)

            // When
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // Then
            verify { getWantToReadBooksUseCase.execute(page = 1) }
            verify { booksObserver.onChanged(emptyList()) }
            verify { loadingObserver.onChanged(true) }
            verify { loadingObserver.onChanged(false) }

            // Verifying that more book cannot be loaded
            sut.onUiEvent(BookContract.UiEvent.LoadMore)
            verify(exactly = 1) { getWantToReadBooksUseCase.execute(page = 1) }
            verify(exactly = 0) { getWantToReadBooksUseCase.execute(page = 2) }

            // Cleanup
            sut.booksLiveData.removeObserver(booksObserver)
            sut.isLoadingLiveData.removeObserver(loadingObserver)
        }

        @Test
        fun `GIVEN error response, WHEN Initialise event, THEN update errorLiveData and clear loading state`() {
            // Given
            val errorMessage = "Network error"
            val exception = RuntimeException(errorMessage)
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.error(exception)

            val errorObserver = mockk<Observer<BookContract.Error?>>(relaxed = true)
            val loadingObserver = mockk<Observer<Boolean>>(relaxed = true)
            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)

            sut.errorLiveData.observeForever(errorObserver)
            sut.isLoadingLiveData.observeForever(loadingObserver)
            sut.booksLiveData.observeForever(booksObserver)

            // When
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // Then
            verify { getWantToReadBooksUseCase.execute(page = 1) }
            verify { errorObserver.onChanged(null) } // Initial value
            verify { errorObserver.onChanged(BookContract.Error.Books(errorMessage)) }
            verify { loadingObserver.onChanged(true) }
            verify { loadingObserver.onChanged(false) }
            verify { booksObserver.onChanged(emptyList()) } // Initial value, no update on error

            // Cleanup
            sut.errorLiveData.removeObserver(errorObserver)
            sut.isLoadingLiveData.removeObserver(loadingObserver)
            sut.booksLiveData.removeObserver(booksObserver)
        }

        @Test
        fun `GIVEN error with null message, WHEN Initialise event, THEN use default error message`() {
            // Given
            val exception = RuntimeException(null as String?)
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.error(exception)

            val errorObserver = mockk<Observer<BookContract.Error?>>(relaxed = true)
            sut.errorLiveData.observeForever(errorObserver)

            // When
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // Then
            verify { errorObserver.onChanged(BookContract.Error.Books("Unknown error")) }

            // Cleanup
            sut.errorLiveData.removeObserver(errorObserver)
        }
    }

    @Nested
    inner class BookClickedEvent {

        @Test
        fun `GIVEN successful response, WHEN BookClicked event, THEN load book details and show bottom sheet`() {
            // Given
            val bookKey = "key"
            val expectedBookDetail = BookDetail(
                title = "Test Book",
                firstPublishDate = "2020",
                latestRevision = "1",
                description = "A test book description",
                subjectPlaces = listOf("New York", "London")
            )
            every { getBookDetailUseCase.execute(bookKey) } returns Single.just(expectedBookDetail)

            val bookDetailObserver = mockk<Observer<BookDetail?>>(relaxed = true)
            val isLoadingDetailsObserver = mockk<Observer<Boolean>>(relaxed = true)
            val showBottomSheetObserver = mockk<Observer<Boolean>>(relaxed = true)
            val errorObserver = mockk<Observer<BookContract.Error?>>(relaxed = true)

            sut.bookDetailLiveData.observeForever(bookDetailObserver)
            sut.isLoadingDetailsLiveData.observeForever(isLoadingDetailsObserver)
            sut.showBottomSheetLiveData.observeForever(showBottomSheetObserver)
            sut.errorLiveData.observeForever(errorObserver)

            // When
            sut.onUiEvent(BookContract.UiEvent.BookClicked(bookKey))

            // Then
            verify { getBookDetailUseCase.execute(bookKey) }
            verify { bookDetailObserver.onChanged(null) } // Initial value
            verify { bookDetailObserver.onChanged(expectedBookDetail) }
            verify { isLoadingDetailsObserver.onChanged(false) } // Initial value
            verify { isLoadingDetailsObserver.onChanged(true) } // Loading started
            verify { isLoadingDetailsObserver.onChanged(false) } // Loading finished
            verify { showBottomSheetObserver.onChanged(false) } // Initial value
            verify { showBottomSheetObserver.onChanged(true) } // Bottom sheet shown
            verify { errorObserver.onChanged(null) } // Initial and cleared error

            // Cleanup
            sut.bookDetailLiveData.removeObserver(bookDetailObserver)
            sut.isLoadingDetailsLiveData.removeObserver(isLoadingDetailsObserver)
            sut.showBottomSheetLiveData.removeObserver(showBottomSheetObserver)
            sut.errorLiveData.removeObserver(errorObserver)
        }

        @Test
        fun `GIVEN error response, WHEN BookClicked event, THEN set error and show bottom sheet`() {
            // Given
            val bookKey = "key"
            val errorMessage = "Failed to fetch book details"
            val exception = RuntimeException(errorMessage)
            every { getBookDetailUseCase.execute(bookKey) } returns Single.error(exception)

            val bookDetailObserver = mockk<Observer<BookDetail?>>(relaxed = true)
            val isLoadingDetailsObserver = mockk<Observer<Boolean>>(relaxed = true)
            val showBottomSheetObserver = mockk<Observer<Boolean>>(relaxed = true)
            val errorObserver = mockk<Observer<BookContract.Error?>>(relaxed = true)

            sut.bookDetailLiveData.observeForever(bookDetailObserver)
            sut.isLoadingDetailsLiveData.observeForever(isLoadingDetailsObserver)
            sut.showBottomSheetLiveData.observeForever(showBottomSheetObserver)
            sut.errorLiveData.observeForever(errorObserver)

            // When
            sut.onUiEvent(BookContract.UiEvent.BookClicked(bookKey))

            // Then
            verify { getBookDetailUseCase.execute(bookKey) }
            verify { bookDetailObserver.onChanged(null) } // Initial value only, no update on error
            verify { errorObserver.onChanged(null) } // Initial value
            verify { errorObserver.onChanged(BookContract.Error.BookDetail(errorMessage)) }
            verify { isLoadingDetailsObserver.onChanged(true) } // Loading started
            verify { isLoadingDetailsObserver.onChanged(false) } // Loading finished
            verify { showBottomSheetObserver.onChanged(true) } // Bottom sheet shown even on error

            // Cleanup
            sut.bookDetailLiveData.removeObserver(bookDetailObserver)
            sut.isLoadingDetailsLiveData.removeObserver(isLoadingDetailsObserver)
            sut.showBottomSheetLiveData.removeObserver(showBottomSheetObserver)
            sut.errorLiveData.removeObserver(errorObserver)
        }
    }

    @Nested
    inner class DismissBottomSheetEvent {

        @Test
        fun `GIVEN bottom sheet is shown with book details, WHEN DismissBottomSheet event, THEN clear state`() {
            // Given
            val bookKey = "key"
            val bookDetail = BookDetail(
                title = "Test Book",
                firstPublishDate = "2020",
                latestRevision = "1",
                description = "Description",
                subjectPlaces = emptyList()
            )
            every { getBookDetailUseCase.execute(bookKey) } returns Single.just(bookDetail)

            val showBottomSheetObserver = mockk<Observer<Boolean>>(relaxed = true)
            val bookDetailObserver = mockk<Observer<BookDetail?>>(relaxed = true)

            sut.showBottomSheetLiveData.observeForever(showBottomSheetObserver)
            sut.bookDetailLiveData.observeForever(bookDetailObserver)

            // First show the bottom sheet with details
            sut.onUiEvent(BookContract.UiEvent.BookClicked(bookKey))

            // When - Dismiss the bottom sheet
            sut.onUiEvent(BookContract.UiEvent.DismissBottomSheet)

            // Then - Verify bottom sheet is hidden and details are cleared
            verify { showBottomSheetObserver.onChanged(false) } // Initial value
            verify { showBottomSheetObserver.onChanged(true) } // Shown when book clicked
            verify { showBottomSheetObserver.onChanged(false) } // Hidden when dismissed
            verify { bookDetailObserver.onChanged(null) } // Initial value
            verify { bookDetailObserver.onChanged(bookDetail) } // Set when book clicked
            verify { bookDetailObserver.onChanged(null) } // Cleared when dismissed

            // Cleanup
            sut.showBottomSheetLiveData.removeObserver(showBottomSheetObserver)
            sut.bookDetailLiveData.removeObserver(bookDetailObserver)
        }
    }

    @Nested
    inner class TryAgainClickedEvent {

        @Test
        fun `GIVEN previous error, WHEN TryAgainClicked event with successful response, THEN clear error and load books`() {
            // Given - First call fails
            val errorMessage = "Network error"
            val exception = RuntimeException(errorMessage)
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.error(exception)

            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)
            val errorObserver = mockk<Observer<BookContract.Error?>>(relaxed = true)
            val loadingObserver = mockk<Observer<Boolean>>(relaxed = true)

            sut.booksLiveData.observeForever(booksObserver)
            sut.errorLiveData.observeForever(errorObserver)
            sut.isLoadingLiveData.observeForever(loadingObserver)

            // Initialize with error
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // Given - Retry will succeed
            val expectedBooks = listOf(
                Book("Book One", "/works/OL123W", "Author One", "url1")
            )
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(expectedBooks)

            // When
            sut.onUiEvent(BookContract.UiEvent.TryAgainClicked)

            // Then
            verify(exactly = 2) { getWantToReadBooksUseCase.execute(page = 1) } // Initial + retry
            verify { booksObserver.onChanged(emptyList()) } // Initial value and cleared before retry
            verify { booksObserver.onChanged(expectedBooks) } // Success after retry
            verify { errorObserver.onChanged(BookContract.Error.Books(errorMessage)) } // Initial error
            verify(atLeast = 2) { errorObserver.onChanged(null) } // Cleared on retry
            verify(atLeast = 2) { loadingObserver.onChanged(true) } // Loading for both attempts

            // Cleanup
            sut.booksLiveData.removeObserver(booksObserver)
            sut.errorLiveData.removeObserver(errorObserver)
            sut.isLoadingLiveData.removeObserver(loadingObserver)
        }

        @Test
        fun `GIVEN previous error, WHEN TryAgainClicked event with error response, THEN show new error`() {
            // Given - First call fails
            val firstError = "Network timeout"
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.error(RuntimeException(firstError))

            val errorObserver = mockk<Observer<BookContract.Error?>>(relaxed = true)
            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)

            sut.errorLiveData.observeForever(errorObserver)
            sut.booksLiveData.observeForever(booksObserver)

            // Initialize with error
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // Given - Retry also fails with different error
            val secondError = "Server error"
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.error(RuntimeException(secondError))

            // When
            sut.onUiEvent(BookContract.UiEvent.TryAgainClicked)

            // Then
            verify(exactly = 2) { getWantToReadBooksUseCase.execute(page = 1) } // Initial + retry
            verify { errorObserver.onChanged(BookContract.Error.Books(firstError)) } // First error
            verify { errorObserver.onChanged(null) } // Cleared before retry
            verify { errorObserver.onChanged(BookContract.Error.Books(secondError)) } // Second error
            verify(atLeast = 2) { booksObserver.onChanged(emptyList()) } // Cleared on retry

            // Cleanup
            sut.errorLiveData.removeObserver(errorObserver)
            sut.booksLiveData.removeObserver(booksObserver)
        }

        @Test
        fun `WHEN TryAgainClicked event, THEN clear books list before retrying`() {
            // Given - First call succeeds with some books
            val initialBooks = listOf(
                Book("Book One", "/works/OL123W", "Author One", "url1")
            )
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(initialBooks)

            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)
            sut.booksLiveData.observeForever(booksObserver)

            // Initialize successfully
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // Given - Retry will return new books
            val newBooks = listOf(
                Book("Book Two", "/works/OL456W", "Author Two", "url2")
            )
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(newBooks)

            // When
            sut.onUiEvent(BookContract.UiEvent.TryAgainClicked)

            // Then
            verify { booksObserver.onChanged(emptyList()) } // Initial value and cleared before retry
            verify { booksObserver.onChanged(initialBooks) } // First load
            verify { booksObserver.onChanged(newBooks) } // After retry

            // Cleanup
            sut.booksLiveData.removeObserver(booksObserver)
        }

        @Test
        fun `WHEN TryAgainClicked event, THEN reset to page 1`() {
            // Given - Initial load
            val page1Books = listOf(Book("Book 1", "key1", "Author 1", "url1"))
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(page1Books)
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // When - Retry is triggered
            sut.onUiEvent(BookContract.UiEvent.TryAgainClicked)

            // Then - Should call page 1 again (not page 2 or any other page)
            verify(exactly = 2) { getWantToReadBooksUseCase.execute(page = 1) }
        }
    }

    @Nested
    inner class LoadMoreEvent {

        @Test
        fun `GIVEN successful initial load, WHEN LoadMore event with successful response, THEN append new books to list`() {
            // Given - Initial load with books
            val page1Books = listOf(
                Book("Book 1", "key1", "Author 1", "url1"),
                Book("Book 2", "key2", "Author 2", "url2")
            )
            val page2Books = listOf(
                Book("Book 3", "key3", "Author 3", "url3"),
                Book("Book 4", "key4", "Author 4", "url4")
            )
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(page1Books)
            every { getWantToReadBooksUseCase.execute(page = 2) } returns Single.just(page2Books)

            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)
            val isLoadingMoreObserver = mockk<Observer<Boolean>>(relaxed = true)
            val paginationErrorObserver = mockk<Observer<String?>>(relaxed = true)

            sut.booksLiveData.observeForever(booksObserver)
            sut.isLoadingMoreLiveData.observeForever(isLoadingMoreObserver)
            sut.paginationError.observeForever(paginationErrorObserver)

            // Initialize
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // When
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // Then
            verify { getWantToReadBooksUseCase.execute(page = 1) }
            verify { getWantToReadBooksUseCase.execute(page = 2) }
            verify { booksObserver.onChanged(emptyList()) } // Initial
            verify { booksObserver.onChanged(page1Books) } // After page 1
            verify { booksObserver.onChanged(page1Books + page2Books) } // After page 2
            verify { isLoadingMoreObserver.onChanged(false) } // Initial
            verify { isLoadingMoreObserver.onChanged(true) } // Loading more started
            verify { isLoadingMoreObserver.onChanged(false) } // Loading more finished
            verify { paginationErrorObserver.onChanged(null) } // Initial and cleared

            // Cleanup
            sut.booksLiveData.removeObserver(booksObserver)
            sut.isLoadingMoreLiveData.removeObserver(isLoadingMoreObserver)
            sut.paginationError.removeObserver(paginationErrorObserver)
        }

        @Test
        fun `GIVEN successful initial load, WHEN LoadMore event returns empty list, THEN prevent further loading`() {
            // Given - Initial load
            val page1Books = listOf(Book("Book 1", "key1", "Author 1", "url1"))
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(page1Books)
            every { getWantToReadBooksUseCase.execute(page = 2) } returns Single.just(emptyList())

            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)

            sut.booksLiveData.observeForever(booksObserver)
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // When - Load more returns empty
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // Then - Books list should not change
            verify { booksObserver.onChanged(page1Books) }

            // When - Try to load more again
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // Then - Should not make another API call
            verify(exactly = 1) { getWantToReadBooksUseCase.execute(page = 2) }
            verify(exactly = 0) { getWantToReadBooksUseCase.execute(page = 3) }

            // Cleanup
            sut.booksLiveData.removeObserver(booksObserver)
        }

        @Test
        fun `WHEN LoadMore event with error, THEN set pagination error and keep existing books`() {
            // Given - Initial load
            val page1Books = listOf(Book("Book 1", "key1", "Author 1", "url1"))
            val errorMessage = "Network error"
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(page1Books)
            every { getWantToReadBooksUseCase.execute(page = 2) } returns Single.error(RuntimeException(errorMessage))

            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)
            val paginationErrorObserver = mockk<Observer<String?>>(relaxed = true)
            val isLoadingMoreObserver = mockk<Observer<Boolean>>(relaxed = true)

            sut.booksLiveData.observeForever(booksObserver)
            sut.paginationError.observeForever(paginationErrorObserver)
            sut.isLoadingMoreLiveData.observeForever(isLoadingMoreObserver)

            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // When
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // Then
            verify { paginationErrorObserver.onChanged(null) } // Initial and cleared before load
            verify { paginationErrorObserver.onChanged(errorMessage) }
            verify { booksObserver.onChanged(page1Books) } // Books remain unchanged
            verify { isLoadingMoreObserver.onChanged(true) }
            verify { isLoadingMoreObserver.onChanged(false) }

            // Cleanup
            sut.booksLiveData.removeObserver(booksObserver)
            sut.paginationError.removeObserver(paginationErrorObserver)
            sut.isLoadingMoreLiveData.removeObserver(isLoadingMoreObserver)
        }

        @Test
        fun `GIVEN already loading more, WHEN LoadMore event, THEN ignore request`() {
            // Given - Initial load
            val page1Books = listOf(Book("Book 1", "key1", "Author 1", "url1"))
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(page1Books)
            every { getWantToReadBooksUseCase.execute(page = 2) } returns Single.never() // Never completes

            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // When - Trigger load more (will start but not complete)
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // When - Try to load more again while first is still loading
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // Then - Should only call page 2 once (debouncing)
            verify(exactly = 1) { getWantToReadBooksUseCase.execute(page = 2) }
        }

        @Test
        fun `GIVEN initial load is loading, WHEN LoadMore event, THEN ignore request`() {
            // Given - Initial load that never completes
            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.never()

            // When - Start initializing (but it won't complete)
            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // When - Try to load more while initial load is still ongoing
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // Then - Should not call page 2
            verify(exactly = 0) { getWantToReadBooksUseCase.execute(page = 2) }
        }

        @Test
        fun `GIVEN multiple successful loads, WHEN LoadMore event called multiple times, THEN increment page counter correctly`() {
            // Given
            val page1Books = listOf(Book("Book 1", "key1", "Author 1", "url1"))
            val page2Books = listOf(Book("Book 2", "key2", "Author 2", "url2"))
            val page3Books = listOf(Book("Book 3", "key3", "Author 3", "url3"))

            every { getWantToReadBooksUseCase.execute(page = 1) } returns Single.just(page1Books)
            every { getWantToReadBooksUseCase.execute(page = 2) } returns Single.just(page2Books)
            every { getWantToReadBooksUseCase.execute(page = 3) } returns Single.just(page3Books)

            val booksObserver = mockk<Observer<List<Book>>>(relaxed = true)
            sut.booksLiveData.observeForever(booksObserver)

            sut.onUiEvent(BookContract.UiEvent.Initialise)

            // When - Load page 2
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // When - Load page 3
            sut.onUiEvent(BookContract.UiEvent.LoadMore)

            // Then
            verify(exactly = 1) { getWantToReadBooksUseCase.execute(page = 1) }
            verify(exactly = 1) { getWantToReadBooksUseCase.execute(page = 2) }
            verify(exactly = 1) { getWantToReadBooksUseCase.execute(page = 3) }
            verify { booksObserver.onChanged(page1Books) }
            verify { booksObserver.onChanged(page1Books + page2Books) }
            verify { booksObserver.onChanged(page1Books + page2Books + page3Books) }

            // Cleanup
            sut.booksLiveData.removeObserver(booksObserver)
        }
    }
}
