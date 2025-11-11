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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
    
    // MVVM: Observe multiple StateFlows
    val books by viewModel.books.collectAsStateWithLifecycle()
    val bookDetail by viewModel.bookDetail.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingDetails by viewModel.isLoadingDetails.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val showBottomSheet by viewModel.showBottomSheet.collectAsStateWithLifecycle()
    
    // Pagination state
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val paginationError by viewModel.paginationError.collectAsStateWithLifecycle()

    BookContent(
        books = books,
        bookDetail = bookDetail,
        isLoading = isLoading,
        isLoadingDetails = isLoadingDetails,
        error = error,
        showBottomSheet = showBottomSheet,
        isLoadingMore = isLoadingMore,
        paginationError = paginationError,
        onUiEvent = viewModel::onUiEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookContent(
    books: List<Book>,
    bookDetail: BookDetail?,
    isLoading: Boolean,
    isLoadingDetails: Boolean,
    error: BookContract.Error?,
    showBottomSheet: Boolean,
    isLoadingMore: Boolean,
    paginationError: String?,
    onUiEvent: (BookContract.UiEvent) -> Unit,
) {
    LaunchedEffect(Unit) {
        onUiEvent(BookContract.UiEvent.Initialise)
    }

    val gridState = rememberLazyGridState()
    val sheetState = rememberModalBottomSheetState()
    
    // Detect when user scrolls near bottom for pagination
    LaunchedEffect(gridState) {
        snapshotFlow { 
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index 
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex != null) {
                val totalItems = gridState.layoutInfo.totalItemsCount
                // Trigger load when user is 3 items from the end
                if (lastVisibleIndex >= totalItems - 3 && !isLoading) {
                    onUiEvent(BookContract.UiEvent.LoadMore)
                }
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error is BookContract.Error.Books -> {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = error.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { onUiEvent(BookContract.UiEvent.TryAgainClicked) }) {
                            Text("Try Again")
                        }
                    }
                }

                books.isNotEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(books) { book ->
                            BookItem(
                                book,
                                Modifier.clickable {
                                    onUiEvent(BookContract.UiEvent.BookClicked(book.key))
                                },
                            )
                        }
                        
                        // Pagination footer - spans full width
                        item(span = { GridItemSpan(2) }) {
                            PaginationFooter(
                                isLoadingMore = isLoadingMore,
                                paginationError = paginationError,
                                onRetry = { onUiEvent(BookContract.UiEvent.RetryPagination) }
                            )
                        }
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { onUiEvent(BookContract.UiEvent.DismissBottomSheet) },
                sheetState = sheetState
            ) {
                BottomSheetContent(
                    bookDetail = bookDetail,
                    isLoading = isLoadingDetails,
                    error = error as? BookContract.Error.BookDetail
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
fun BottomSheetContent(
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            bookDetail != null -> {
                BookDetailContent(bookDetail)
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
fun BookDetailContent(bookDetail: BookDetail) {
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

@Composable
fun PaginationFooter(
    isLoadingMore: Boolean,
    paginationError: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingMore -> {
                CircularProgressIndicator()
            }
            paginationError != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = paginationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
