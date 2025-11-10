package com.tinnovakovic.mybooks.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BookScreen() {

    val viewModel = hiltViewModel<BookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BookContent(uiState, viewModel::onUiEvent)

}

@Composable
fun BookContent(
    uiState: BookContract.UiState,
    uiEvent: (BookContract.UiEvent) -> Unit,
) {

    LaunchedEffect(true) {
        uiEvent(BookContract.UiEvent.Initialise)
    }


}

