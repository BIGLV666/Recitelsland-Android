package com.biglv.recitelsland.core.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeController(
    private val store: AppSettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _mode = MutableStateFlow(ThemeMode.System)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    init {
        scope.launch {
            store.themeMode.collect { _mode.value = it }
        }
    }

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        scope.launch { store.setThemeMode(mode) }
    }
}
