package com.biglv.recitelsland.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

inline fun <reified VM : ViewModel> simpleViewModelFactory(
    crossinline create: () -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return create() as T
    }
}
