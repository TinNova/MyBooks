package com.tinnovakovic.mybooks

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.tinnovakovic.mybooks.presentation.BookContentErrorPreview
import com.tinnovakovic.mybooks.presentation.BookContentSuccessPreview
import com.tinnovakovic.mybooks.presentation.PreviewScreenFormats

class BookContentScreenshots {

    @PreviewTest
    @PreviewScreenFormats
    @Composable
    private fun BookContentSuccess() {
        BookContentSuccessPreview()
    }

    @PreviewTest
    @PreviewScreenFormats
    @Composable
    private fun BookContentError() {
        BookContentErrorPreview()
    }

}
