package com.tinnovakovic.mybooks

import android.app.Application
import javax.inject.Inject

class ContextWrapperImpl @Inject constructor(private val application: Application): ContextWrapper {

    override fun getContext(): Application = application
}