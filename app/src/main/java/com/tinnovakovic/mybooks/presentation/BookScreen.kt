package com.tinnovakovic.mybooks.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.tinnovakovic.mybooks.domain.models.Book
import com.tinnovakovic.mybooks.domain.models.BookDetail

@Composable
fun BookScreen() {

    val viewModel = hiltViewModel<BookViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BookContent(uiState, viewModel::onUiEvent)

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookContent(
    uiState: BookContract.UiState,
    uiEvent: (BookContract.UiEvent) -> Unit,
) {

    LaunchedEffect(true) {
        uiEvent(BookContract.UiEvent.Initialise)
    }

    val sheetState = rememberModalBottomSheetState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error is BookContract.Error.Books -> {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { uiEvent(BookContract.UiEvent.TryAgainClicked) }) {
                            Text("Try Again")
                        }
                    }
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
                            BookItem(
                                book,
                                Modifier.clickable {
                                    uiEvent(BookContract.UiEvent.BookClicked(book.key))
                                },
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { uiEvent(BookContract.UiEvent.DismissBottomSheet) },
                sheetState = sheetState
            ) {
                BookDetailContent(
                    bookDetail = uiState.bookDetail,
                    isLoading = uiState.isLoadingDetails,
                    error = uiState.error as? BookContract.Error.BookDetail
                )
            }
        }
    }
}

@Composable
fun BookItem(book: Book, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
            text = book.authorNames,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun BookDetailContent(
    bookDetail: BookDetail?,
    isLoading: Boolean,
    error: BookContract.Error.BookDetail?

) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            bookDetail != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = bookDetail.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (bookDetail.firstPublishDate.isNotEmpty()) {
                        DetailRow(label = "First Published", value = bookDetail.firstPublishDate)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (bookDetail.latestRevision.isNotEmpty()) {
                        DetailRow(label = "Latest Revision", value = bookDetail.latestRevision)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (bookDetail.description.isNotEmpty()) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bookDetail.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (bookDetail.subjectPlaces.isNotEmpty()) {
                        Text(
                            text = "Subject Places",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bookDetail.subjectPlaces.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            error != null -> {
                    Text(
                        text = error.message,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium
                    )
            }

            else -> {
                Text(
                    text = "No details available",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
