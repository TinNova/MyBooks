package com.tinnovakovic.mybooks.presentation

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tinnovakovic.mybooks.domain.GetBookDetailUseCase
import com.tinnovakovic.mybooks.domain.GetWantToReadBooksUseCase
import com.tinnovakovic.mybooks.domain.models.Book
import com.tinnovakovic.mybooks.domain.models.BookDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val getWantToReadBooksUseCase: GetWantToReadBooksUseCase,
    private val getBookDetailUseCase: GetBookDetailUseCase,
) : BookContract, ViewModel() {

    private var initialised = false
    private val compositeDisposable = CompositeDisposable()

    // MVVM: Multiple LiveData instances instead of single UiState
    private val _booksLiveData = MutableLiveData<List<Book>>(emptyList())
    val booksLiveData: LiveData<List<Book>> = _booksLiveData
    private val _bookDetailLiveData = MutableLiveData<BookDetail?>(null)
    val bookDetailLiveData: LiveData<BookDetail?> = _bookDetailLiveData
    private val _isLoadingLiveData = MutableLiveData(false)
    val isLoadingLiveData: LiveData<Boolean> = _isLoadingLiveData
    private val _isLoadingDetailsLiveData = MutableLiveData(false)
    val isLoadingDetailsLiveData: LiveData<Boolean> = _isLoadingDetailsLiveData
    private val _errorLiveData = MutableLiveData<BookContract.Error?>(null)
    val errorLiveData: LiveData<BookContract.Error?> = _errorLiveData
    private val _showBottomSheetLiveData = MutableLiveData(false)
    val showBottomSheetLiveData: LiveData<Boolean> = _showBottomSheetLiveData
    private val _isLoadingMoreLiveData = MutableLiveData(false)
    val isLoadingMoreLiveData: LiveData<Boolean> = _isLoadingMoreLiveData
    private val _paginationError = MutableLiveData<String?>(null)
    val paginationError: LiveData<String?> = _paginationError
    private var _canLoadMore: Boolean = true
    private var _currentPage = 1

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
        _isLoadingLiveData.value = true
        _currentPage = 1
        
        val disposable = getWantToReadBooksUseCase.execute(page = 1)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { books ->
                    _booksLiveData.value = books
                    _isLoadingLiveData.value = false
                    _errorLiveData.value = null
                    _canLoadMore = books.isNotEmpty()
                },
                { error ->
                    _isLoadingLiveData.value = false
                    _errorLiveData.value = BookContract.Error.Books(error.message ?: "Unknown error")
                }
            )
        compositeDisposable.add(disposable)
    }

    private fun retryInitialLoad() {
        _booksLiveData.value = emptyList()
        _errorLiveData.value = null
        getWantToReadBooks()
    }

    private fun loadNextPage() {
        // Debouncing: prevent multiple simultaneous loads
        if (_isLoadingMoreLiveData.value == true || !_canLoadMore || _isLoadingLiveData.value == true) {
            return
        }

        _isLoadingMoreLiveData.value = true
        _paginationError.value = null
        val nextPage = _currentPage + 1
        
        val disposable = getWantToReadBooksUseCase.execute(page = nextPage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { newBooks ->
                    if (newBooks.isEmpty()) {
                        // No more data to load
                        _canLoadMore = false
                    } else {
                        // Append new books to existing list
                        _booksLiveData.value = _booksLiveData.value.orEmpty() + newBooks
                        _currentPage = nextPage
                    }
                    _isLoadingMoreLiveData.value = false
                },
                { error ->
                    _isLoadingMoreLiveData.value = false
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
        _isLoadingDetailsLiveData.value = true
        _showBottomSheetLiveData.value = true
        
        val disposable = getBookDetailUseCase.execute(key)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { bookDetail ->
                    _bookDetailLiveData.value = bookDetail
                    _isLoadingDetailsLiveData.value = false
                    _errorLiveData.value = null
                },
                { error ->
                    _isLoadingDetailsLiveData.value = false
                    _errorLiveData.value = BookContract.Error.BookDetail(error.message ?: "Failed to load book details")
                }
            )
        compositeDisposable.add(disposable)
    }

    private fun dismissBottomSheet() {
        _showBottomSheetLiveData.value = false
        _bookDetailLiveData.value = null
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

}


//TODO:
// - Clean the code
// - Write Unit Tests
// - Write Compose Tests in Robolectric
// - Check against ClearScore and JsonSpeedRun Apps
// - Error Mapper contains hardcoded strings, add to stringRes and create a contextWrapper to access them, then write tests
//
//Done:
// - Display BookDetail in bottomSheet
// - Handle offline and network errors in domain layer
// - Refactor to MVVM with event-based interactions
// - Handle pagination

















