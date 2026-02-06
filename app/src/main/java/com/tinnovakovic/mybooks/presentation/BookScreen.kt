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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Devices.PHONE
import androidx.compose.ui.tooling.preview.Devices.PIXEL_FOLD
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.tinnovakovic.mybooks.domain.models.Book
import com.tinnovakovic.mybooks.domain.models.BookDetail

@Composable
fun BookScreen() {
    val viewModel = hiltViewModel<BookViewModel>()

    val books by viewModel.booksLiveData.observeAsState(emptyList())
    val bookDetail by viewModel.bookDetailLiveData.observeAsState()
    val isLoading by viewModel.isLoadingLiveData.observeAsState(false)
    val isLoadingDetails by viewModel.isLoadingDetailsLiveData.observeAsState(false)
    val error by viewModel.errorLiveData.observeAsState()
    val showBottomSheet by viewModel.showBottomSheetLiveData.observeAsState(false)

    // Pagination state
    val isLoadingMore by viewModel.isLoadingMoreLiveData.observeAsState(false)
    val paginationError by viewModel.paginationError.observeAsState()

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
                if (lastVisibleIndex >= totalItems - 3) {
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

@Preview(showBackground = true, name = "Loading State")
@Composable
fun BookContentLoadingPreview() {
    BookContent(
        books = emptyList(),
        bookDetail = null,
        isLoading = true,
        isLoadingDetails = false,
        error = null,
        showBottomSheet = false,
        isLoadingMore = false,
        paginationError = null,
        onUiEvent = {}
    )
}

@PreviewScreenFormats
@Composable
fun BookContentErrorPreview() {
    BookContent(
        books = emptyList(),
        bookDetail = null,
        isLoading = false,
        isLoadingDetails = false,
        error = BookContract.Error.Books("Failed to load books. Please check your internet connection."),
        showBottomSheet = false,
        isLoadingMore = false,
        paginationError = null,
        onUiEvent = {}
    )
}

@PreviewScreenFormats
@Composable
fun BookContentSuccessPreview() {
    val sampleBooks = listOf(
        Book(
            title = "The Lord of the Rings",
            key = "/works/OL27448W",
            authorNames = "J.R.R. Tolkien",
            coverUrl = "https://covers.openlibrary.org/b/id/8234024-L.jpg"
        ),
        Book(
            title = "Harry Potter and the Philosopher's Stone",
            key = "/works/OL82563W",
            authorNames = "J.K. Rowling",
            coverUrl = "https://covers.openlibrary.org/b/id/10521270-L.jpg"
        ),
        Book(
            title = "1984",
            key = "/works/OL1168007W",
            authorNames = "George Orwell",
            coverUrl = "https://covers.openlibrary.org/b/id/7222246-L.jpg"
        ),
        Book(
            title = "To Kill a Mockingbird",
            key = "/works/OL16304310W",
            authorNames = "Harper Lee",
            coverUrl = "https://covers.openlibrary.org/b/id/8228691-L.jpg"
        )
    )

    BookContent(
        books = sampleBooks,
        bookDetail = null,
        isLoading = false,
        isLoadingDetails = false,
        error = null,
        showBottomSheet = false,
        isLoadingMore = false,
        paginationError = null,
        onUiEvent = {}
    )
}

@Preview(showBackground = true, name = "Books with Pagination Loading")
@Composable
fun BookContentPaginationLoadingPreview() {
    val sampleBooks = listOf(
        Book(
            title = "The Great Gatsby",
            key = "/works/OL468431W",
            authorNames = "F. Scott Fitzgerald",
            coverUrl = "https://covers.openlibrary.org/b/id/7222168-L.jpg"
        ),
        Book(
            title = "Pride and Prejudice",
            key = "/works/OL66554W",
            authorNames = "Jane Austen",
            coverUrl = "https://covers.openlibrary.org/b/id/8235657-L.jpg"
        )
    )

    BookContent(
        books = sampleBooks,
        bookDetail = null,
        isLoading = false,
        isLoadingDetails = false,
        error = null,
        showBottomSheet = false,
        isLoadingMore = true,
        paginationError = null,
        onUiEvent = {}
    )
}

@Preview(showBackground = true, name = "Books with Pagination Error")
@Composable
fun BookContentPaginationErrorPreview() {
    val sampleBooks = listOf(
        Book(
            title = "The Catcher in the Rye",
            key = "/works/OL3335920W",
            authorNames = "J.D. Salinger",
            coverUrl = "https://covers.openlibrary.org/b/id/8234024-L.jpg"
        ),
        Book(
            title = "Brave New World",
            key = "/works/OL64465W",
            authorNames = "Aldous Huxley",
            coverUrl = "https://covers.openlibrary.org/b/id/7222168-L.jpg"
        )
    )

    BookContent(
        books = sampleBooks,
        bookDetail = null,
        isLoading = false,
        isLoadingDetails = false,
        error = null,
        showBottomSheet = false,
        isLoadingMore = false,
        paginationError = "Failed to load more books",
        onUiEvent = {}
    )
}

@Preview(showBackground = true, name = "Book Detail Bottom Sheet")
@Composable
fun BookContentWithBottomSheetPreview() {
    val sampleBooks = listOf(
        Book(
            title = "The Hobbit",
            key = "/works/OL27482W",
            authorNames = "J.R.R. Tolkien",
            coverUrl = "https://covers.openlibrary.org/b/id/8234024-L.jpg"
        )
    )

    val sampleBookDetail = BookDetail(
        title = "The Hobbit",
        firstPublishDate = "September 21, 1937",
        latestRevision = "2024",
        description = "A fantasy novel about the adventure of Bilbo Baggins, a hobbit who lives in a comfortable home in the Shire. One day, the wizard Gandalf and a group of dwarves arrive at his door and convince him to join them on an epic quest to reclaim the Lonely Mountain from the dragon Smaug.",
        subjectPlaces = listOf("Middle-earth", "The Shire", "Lonely Mountain", "Rivendell")
    )

    BookContent(
        books = sampleBooks,
        bookDetail = sampleBookDetail,
        isLoading = false,
        isLoadingDetails = false,
        error = null,
        showBottomSheet = true,
        isLoadingMore = false,
        paginationError = null,
        onUiEvent = {}
    )
}

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "Phone", device = PHONE, showSystemUi = true)
@Preview(name = "Phone 150%", device = PHONE, showSystemUi = true, fontScale = 2f)
@Preview(name = "Unfolded Foldable", device = PIXEL_FOLD, showSystemUi = true)
@Preview(name = "Unfolded Foldable", device = PIXEL_FOLD, showSystemUi = true, fontScale = 2f)
annotation class PreviewScreenFormats