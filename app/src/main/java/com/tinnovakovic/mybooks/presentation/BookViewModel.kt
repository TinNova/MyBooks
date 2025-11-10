package com.tinnovakovic.mybooks.presentation

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import com.tinnovakovic.mybooks.data.BookRepo
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
    private val bookRepo: BookRepo,
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
        val disposable = bookRepo.getWantToReadBooks(page = 1)
            .subscribeOn(Schedulers.io()) //Can I use this directly? Or should it be injected?
            .observeOn(AndroidSchedulers.mainThread()) //Understand what changed when these are swapped
            .subscribe(
                { result -> //should I implement coding on rails?
                    // Handle success - update UI state with books
                    println("Books fetched successfully: ${result.entries.size} books")
                },
                { error ->
                    // Handle error
                    println("Error fetching books: ${error.message}")
                }
            )
        compositeDisposable.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    companion object {
        fun initialUiState() = BookContract.UiState(
            greeting = "Hello World",
        )
    }
}
