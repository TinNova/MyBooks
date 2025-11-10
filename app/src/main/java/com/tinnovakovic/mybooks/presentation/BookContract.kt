package com.tinnovakovic.mybooks.presentation

import com.tinnovakovic.mybooks.domain.models.Book

interface BookContract {

    data class UiState(
        val books: List<Book> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    sealed class UiEvent {
        data object Initialise: UiEvent()
    }
}
