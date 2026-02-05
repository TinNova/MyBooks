package com.tinnovakovic.mybooks

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.tools.screenshot.PreviewTest
import com.tinnovakovic.mybooks.domain.models.Book
import com.tinnovakovic.mybooks.domain.models.BookDetail
import com.tinnovakovic.mybooks.presentation.BookContent
import com.tinnovakovic.mybooks.presentation.BookContract

@PreviewTest
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

@PreviewTest
@Preview(showBackground = true, name = "Error State")
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

