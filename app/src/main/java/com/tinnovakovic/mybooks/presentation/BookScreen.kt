package com.tinnovakovic.mybooks.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

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

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    Text(
                        text = "Error: ${uiState.error}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.books.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.books) { book ->
                            BookItem(book)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookItem(book: BookContract.BookUi) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = book.authors,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
