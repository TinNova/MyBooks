package com.tinnovakovic.mybooks.presentation

import com.tinnovakovic.mybooks.domain.models.Book
import com.tinnovakovic.mybooks.domain.models.BookDetail

interface BookContract {

    sealed class Error {
        data class Books(val message: String): Error()
        data class BookDetail(val message: String): Error()
    }

    sealed class UiEvent {
        data object Initialise : UiEvent()
        data object TryAgainClicked : UiEvent()
        data class BookClicked(val key: String) : UiEvent()
        data object DismissBottomSheet : UiEvent()
        data object LoadMore : UiEvent()
        data object RetryPagination : UiEvent()
    }
}
