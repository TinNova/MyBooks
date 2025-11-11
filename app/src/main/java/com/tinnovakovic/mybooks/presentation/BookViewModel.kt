package com.tinnovakovic.mybooks.presentation

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import com.tinnovakovic.mybooks.domain.GetBookDetailUseCase
import com.tinnovakovic.mybooks.domain.GetWantToReadBooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val getWantToReadBooksUseCase: GetWantToReadBooksUseCase,
    private val getBookDetailUseCase: GetBookDetailUseCase,
) : BookContract, ViewModel() {

    private var initialised = false
    private val compositeDisposable = CompositeDisposable()

    // MVVM: Multiple StateFlows instead of single UiState
    private val _books = MutableStateFlow<List<com.tinnovakovic.mybooks.domain.models.Book>>(emptyList())
    val books: StateFlow<List<com.tinnovakovic.mybooks.domain.models.Book>> = _books.asStateFlow()

    private val _bookDetail = MutableStateFlow<com.tinnovakovic.mybooks.domain.models.BookDetail?>(null)
    val bookDetail: StateFlow<com.tinnovakovic.mybooks.domain.models.BookDetail?> = _bookDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails: StateFlow<Boolean> = _isLoadingDetails.asStateFlow()

    private val _error = MutableStateFlow<BookContract.Error?>(null)
    val error: StateFlow<BookContract.Error?> = _error.asStateFlow()

    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet.asStateFlow()

    // Pagination state
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _paginationError = MutableStateFlow<String?>(null)
    val paginationError: StateFlow<String?> = _paginationError.asStateFlow()

    @MainThread
    fun initialise() {
        if (initialised) return
        initialised = true
        getWantToReadBooks()
    }

    fun onUiEvent(event: BookContract.UiEvent) {
        when (event) {
            BookContract.UiEvent.Initialise -> initialise()
            is BookContract.UiEvent.BookClicked -> getBookDetails(event.key)
            BookContract.UiEvent.DismissBottomSheet -> dismissBottomSheet()
            BookContract.UiEvent.TryAgainClicked -> retryInitialLoad()
            BookContract.UiEvent.LoadMore -> loadNextPage()
            BookContract.UiEvent.RetryPagination -> retryPagination()
        }
    }

    private fun getWantToReadBooks() {
        _isLoading.value = true
        _currentPage.value = 1
        
        val disposable = getWantToReadBooksUseCase.execute(page = 1)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { books ->
                    _books.value = books
                    _isLoading.value = false
                    _error.value = null
                    _canLoadMore.value = books.isNotEmpty()
                },
                { error ->
                    _isLoading.value = false
                    _error.value = BookContract.Error.Books(error.message ?: "Unknown error")
                }
            )
        compositeDisposable.add(disposable)
    }

    private fun retryInitialLoad() {
        _books.value = emptyList()
        _error.value = null
        getWantToReadBooks()
    }

    private fun loadNextPage() {
        // Debouncing: prevent multiple simultaneous loads
        if (_isLoadingMore.value || !_canLoadMore.value || _isLoading.value) {
            return
        }

        _isLoadingMore.value = true
        _paginationError.value = null
        val nextPage = _currentPage.value + 1
        
        val disposable = getWantToReadBooksUseCase.execute(page = nextPage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { newBooks ->
                    if (newBooks.isEmpty()) {
                        // No more data to load
                        _canLoadMore.value = false
                    } else {
                        // Append new books to existing list
                        _books.value = _books.value + newBooks
                        _currentPage.value = nextPage
                    }
                    _isLoadingMore.value = false
                },
                { error ->
                    _isLoadingMore.value = false
                    _paginationError.value = error.message ?: "Failed to load more books"
                }
            )
        compositeDisposable.add(disposable)
    }

    private fun retryPagination() {
        _paginationError.value = null
        loadNextPage()
    }

    private fun getBookDetails(key: String) {
        _isLoadingDetails.value = true
        _showBottomSheet.value = true
        
        val disposable = getBookDetailUseCase.execute(key)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { bookDetail ->
                    _bookDetail.value = bookDetail
                    _isLoadingDetails.value = false
                    _error.value = null
                },
                { error ->
                    _isLoadingDetails.value = false
                    _error.value = BookContract.Error.BookDetail(error.message ?: "Failed to load book details")
                }
            )
        compositeDisposable.add(disposable)
    }

    private fun dismissBottomSheet() {
        _showBottomSheet.value = false
        _bookDetail.value = null
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

}


//TODO:
// - Clean the code
// - Handle pagination
// - Write Unit Tests
// - Write Compose Tests in Robolectric
// - Check against ClearScore and JsonSpeedRun Apps
//
//Done:
// - Display BookDetail in bottomSheet
// - Handle offline and network errors in domain layer
// - Refactor to MVVM with event-based interactions
















