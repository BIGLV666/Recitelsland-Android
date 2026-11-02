package com.biglv.recitelsland.feature.reviewplans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.data.model.ReviewItem
import com.biglv.recitelsland.data.repo.ReciteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewPlansUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val items: List<ReviewItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val message: String? = null,
)

class ReviewPlansViewModel(
    private val repository: ReciteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewPlansUiState())
    val state: StateFlow<ReviewPlansUiState> = _state.asStateFlow()

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

    fun remove(reviewId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteReview(reviewId) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            items = it.items.filterNot { item -> item.reviewId == reviewId },
                            message = "已移出复习计划",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = error.message ?: "移出复习计划失败") }
                }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun load(reset: Boolean, refreshing: Boolean = false) {
        val current = _state.value
        val cursor = if (reset) null else current.nextCursor
        _state.update {
            if (reset) {
                it.copy(
                    loading = !refreshing,
                    refreshing = refreshing,
                    items = if (refreshing) it.items else emptyList(),
                    nextCursor = null,
                    hasMore = false,
                )
            } else {
                it.copy(loadingMore = true)
            }
        }

        viewModelScope.launch {
            runCatching { repository.reviews(cursor, PAGE_SIZE) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            loadingMore = false,
                            items = if (reset) page.items else it.items + page.items,
                            nextCursor = page.nextCursor,
                            hasMore = page.hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            loadingMore = false,
                            message = error.message ?: "复习计划加载失败",
                        )
                    }
                }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
