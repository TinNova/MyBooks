package com.tinnovakovic.mybooks.presentation

import com.tinnovakovic.mybooks.domain.models.Book
import com.tinnovakovic.mybooks.domain.models.BookDetail

interface BookContract {

    data class UiState(
        val books: List<Book> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val bookDetail: BookDetail? = null,
        val isLoadingDetails: Boolean = false
    )

    sealed class UiEvent {
        data object Initialise : UiEvent()
        data class BookClicked(val key: String) : UiEvent()
    }
}
