package com.tinnovakovic.mybooks.presentation

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import com.tinnovakovic.mybooks.data.BookRepo
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
                        error = error.message ?: "Unknown error"
                    )
                }
            )
        compositeDisposable.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    companion object {
        fun initialUiState() = BookContract.UiState()
    }
}
