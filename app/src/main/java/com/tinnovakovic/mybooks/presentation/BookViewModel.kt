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

    @MainThread
    fun initialise() {
        if (initialised) return
        initialised = true

        getWantToReadBooks()

    }

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<BookContract.UiState> = _uiState.asStateFlow()

    fun onUiEvent(event: BookContract.UiEvent) {
        when (event) {
            BookContract.UiEvent.Initialise -> initialise()
            is BookContract.UiEvent.BookClicked -> getBookDetails(event.key)
            BookContract.UiEvent.DismissBottomSheet -> dismissBottomSheet()
            BookContract.UiEvent.TryAgainClicked -> getWantToReadBooks()
        }
    }

    private fun getWantToReadBooks() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        val disposable = getWantToReadBooksUseCase.execute(page = 1)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { books ->
                    _uiState.value = _uiState.value.copy(
                        books = books,
                        isLoading = false,
                        error = null
                    )
                },
                { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = BookContract.Error.Books(error.message ?: "Unknown error")
                    )
                }
            )
        compositeDisposable.add(disposable)
    }

    private fun getBookDetails(key: String) {
        _uiState.value = _uiState.value.copy(isLoadingDetails = true, showBottomSheet = true)
        
        val disposable = getBookDetailUseCase.execute(key)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { bookDetail ->
                    _uiState.value = _uiState.value.copy(
                        bookDetail = bookDetail,
                        isLoadingDetails = false,
                        error = null
                    )
                },
                { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetails = false,
                        error = BookContract.Error.BookDetail(error.message ?: "Failed to load book details")
                    )
                }
            )
        compositeDisposable.add(disposable)
    }

    private fun dismissBottomSheet() {
        _uiState.value = _uiState.value.copy(
            showBottomSheet = false,
            bookDetail = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    companion object {
        fun initialUiState() = BookContract.UiState()
    }
}


//TODO:
// - Clean the code
// - Handle pagination
// - Write Unit Tests
// - Write Compose Tests in Robolectric
// - Check against ClearScore and JsonSpeedRun Apps
// - Refactor to MVVM

//Done
// - Display BookDetail in bottomSheet
// - Handle offline and network errors in domain layer
















