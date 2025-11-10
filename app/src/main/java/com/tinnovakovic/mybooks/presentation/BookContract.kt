package com.tinnovakovic.mybooks.presentation

interface BookContract {

    data class UiState(
        val greeting: String,
    )

    sealed class UiEvent {
        data object Initialise: UiEvent()
    }
}
