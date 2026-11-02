package com.biglv.recitelsland.feature.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.data.model.AnswerFilter
import com.biglv.recitelsland.data.model.AnswerRecord
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.repo.ReciteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val records: List<AnswerRecord> = emptyList(),
    val filter: AnswerFilter = AnswerFilter.All,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val selected: AnswerRecord? = null,
    val selectedQuestion: Question? = null,
    val detailLoading: Boolean = false,
    val message: String? = null,
) {
    val correctCount: Int get() = records.count { it.correct }
    val incorrectCount: Int get() = records.size - correctCount
    val accuracy: Int get() = if (records.isEmpty()) 0 else correctCount * 100 / records.size
}

class RecordsViewModel(
    private val repository: ReciteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordsUiState())
    val state: StateFlow<RecordsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(reset = true)
    }

    fun refresh() {
        if (_state.value.loading || _state.value.refreshing) return
        load(reset = true, refreshing = true)
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.refreshing || current.loadingMore || !current.hasMore) return
        load(reset = false)
    }

    fun setFilter(filter: AnswerFilter) {
        if (_state.value.filter == filter) return
        _state.update { it.copy(filter = filter) }
        load(reset = true)
    }

    fun openRecord(record: AnswerRecord) {
        _state.update {
            it.copy(selected = record, selectedQuestion = null, detailLoading = true)
        }
        viewModelScope.launch {
            runCatching { repository.question(record.question.id) }
                .onSuccess { question ->
                    _state.update {
                        it.copy(
                            selectedQuestion = question,
                            detailLoading = false,
                            message = if (question == null) "题目不存在" else it.message,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            detailLoading = false,
                            message = error.message ?: "题目加载失败",
                        )
                    }
                }
        }
    }

    fun closeSheet() {
        _state.update { it.copy(selected = null, selectedQuestion = null, detailLoading = false) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun load(reset: Boolean, refreshing: Boolean = false) {
        val current = _state.value
        if (!reset && (current.loadingMore || !current.hasMore)) return

        val cursor = if (reset) null else current.nextCursor
        _state.update {
            if (reset) {
                it.copy(
                    loading = !refreshing,
                    refreshing = refreshing,
                    records = if (refreshing) it.records else emptyList(),
                    nextCursor = null,
                    hasMore = false,
                )
            } else {
                it.copy(loadingMore = true)
            }
        }

        if (reset) {
            loadJob?.cancel()
        }
        loadJob = viewModelScope.launch {
            runCatching {
                repository.answerRecords(
                    cursor = cursor,
                    limit = PAGE_SIZE,
                    filter = current.filter,
                )
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        records = if (reset) page.items else it.records + page.items,
                        nextCursor = page.nextCursor,
                        hasMore = page.hasMore,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        message = error.message ?: "记录加载失败",
                    )
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
