package com.tinnovakovic.mybooks

import android.app.Application

interface ContextWrapper {

    fun getContext(): Application
}