package com.biglv.recitelsland.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.data.model.FavoriteItem
import com.biglv.recitelsland.data.repo.ReciteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val items: List<FavoriteItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val message: String? = null,
)

class FavoritesViewModel(
    private val repository: ReciteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    init {
        load(reset = true)
    }

    fun refresh() {
        if (_state.value.loading || _state.value.refreshing) return
        load(reset = true, refreshing = true)
    }

    fun loadMore() {
        val current = _state.value
        if (current.loadingMore || current.loading || current.refreshing || !current.hasMore) return
        load(reset = false)
    }

    fun remove(favoriteId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteFavorite(favoriteId) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            items = it.items.filterNot { item -> item.favoriteId == favoriteId },
                            message = "已取消收藏",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(message = error.message ?: "取消收藏失败") }
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
            runCatching { repository.favorites(cursor, PAGE_SIZE) }
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
                            message = error.message ?: "收藏加载失败",
                        )
                    }
                }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
