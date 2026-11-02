package com.biglv.recitelsland.feature.feedback

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.BuildConfig
import com.biglv.recitelsland.data.model.FeedbackItem
import com.biglv.recitelsland.data.repo.FeedbackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val type: String = "bug",
    val content: String = "",
    val contact: String = "",
    val appVersion: String = BuildConfig.VERSION_NAME,
    val device: String = "android ${Build.MANUFACTURER} ${Build.MODEL}".trim().take(32),
    val sending: Boolean = false,
    val loading: Boolean = false,
    val items: List<FeedbackItem> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val message: String? = null,
) {
    val canLoadMore: Boolean get() = items.size < total
}

class FeedbackViewModel(
    private val repository: FeedbackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    init {
        loadMine(reset = true)
    }

    fun setType(type: String) = _state.update { it.copy(type = type) }
    fun setContent(value: String) = _state.update { it.copy(content = value) }
    fun setContact(value: String) = _state.update { it.copy(contact = value) }
    fun setAppVersion(value: String) = _state.update { it.copy(appVersion = value) }
    fun setDevice(value: String) = _state.update { it.copy(device = value) }

    fun submit() {
        val current = _state.value
        if (current.sending) return
        if (current.content.trim().isEmpty()) {
            _state.update { it.copy(message = "请填写反馈内容") }
            return
        }
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            runCatching {
                repository.create(
                    type = current.type,
                    content = current.content.trim(),
                    contact = current.contact.trim(),
                    appVersion = current.appVersion.trim(),
                    platform = current.device.trim(),
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        sending = false,
                        content = "",
                        contact = "",
                        message = "反馈已提交",
                    )
                }
                loadMine(reset = true)
            }.onFailure { error ->
                _state.update {
                    it.copy(sending = false, message = error.message ?: "反馈提交失败")
                }
            }
        }
    }

    fun loadMore() {
        if (_state.value.loading || !_state.value.canLoadMore) return
        loadMine(reset = false)
    }

    fun loadMine(reset: Boolean) {
        val current = _state.value
        if (current.loading) return
        val page = if (reset) 1 else current.page + 1
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { repository.mine(page, current.limit) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            loading = false,
                            page = result.page,
                            total = result.total,
                            items = if (reset) result.items else it.items + result.items,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, message = error.message ?: "反馈记录加载失败")
                    }
                }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
