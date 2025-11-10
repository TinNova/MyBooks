package com.tinnovakovic.mybooks.presentation

interface BookContract {

    data class UiState(
        val books: List<BookUi> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    data class BookUi(
        val title: String,
        val authors: String,
        val coverUrl: String
    )

    sealed class UiEvent {
        data object Initialise: UiEvent()
    }
}
