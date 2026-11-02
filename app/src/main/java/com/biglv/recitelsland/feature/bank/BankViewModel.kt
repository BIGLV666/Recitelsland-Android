package com.biglv.recitelsland.feature.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionFilter
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.data.model.TagItem
import com.biglv.recitelsland.data.repo.ReciteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BankUiState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val questions: List<Question> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val filter: QuestionFilter = QuestionFilter.All,
    val selectedTag: String? = null,
    val query: String = "",
    val searchVisible: Boolean = false,
    val tagsExpanded: Boolean = false,
    val tags: List<TagItem> = emptyList(),
    val tagNextCursor: String? = null,
    val hasMoreTags: Boolean = false,
    val loadingTags: Boolean = false,
    val message: String? = null,
) {
    val visibleQuestions: List<Question>
        get() = when (filter) {
            QuestionFilter.All -> questions
            QuestionFilter.MultipleChoice -> questions.filter { it.type == QuestionType.MultipleChoice }
            QuestionFilter.TrueFalse -> questions.filter { it.type == QuestionType.TrueFalse }
            QuestionFilter.Reading -> questions.filter { it.type == QuestionType.Reading }
        }

    fun countOf(filter: QuestionFilter): Int = when (filter) {
        QuestionFilter.All -> questions.size
        QuestionFilter.MultipleChoice -> questions.count { it.type == QuestionType.MultipleChoice }
        QuestionFilter.TrueFalse -> questions.count { it.type == QuestionType.TrueFalse }
        QuestionFilter.Reading -> questions.count { it.type == QuestionType.Reading }
    }
}

class BankViewModel(
    private val repository: ReciteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BankUiState())
    val state: StateFlow<BankUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadQuestions(reset = true)
        loadTags(cursor = null)
    }

    fun loadQuestions(reset: Boolean) {
        val current = _state.value
        if (reset) {
            searchJob?.cancel()
        } else if (current.loadingMore || !current.hasMore || current.loading) {
            return
        }

        val cursor = if (reset) null else current.nextCursor
        _state.update {
            if (reset) {
                it.copy(loading = true, nextCursor = null, hasMore = false, questions = emptyList())
            } else {
                it.copy(loadingMore = true)
            }
        }

        if (reset) {
            loadJob?.cancel()
        }
        loadJob = viewModelScope.launch {
            runCatching {
                repository.questions(
                    cursor = cursor,
                    limit = PAGE_SIZE,
                    tag = current.selectedTag,
                    keyword = current.query,
                )
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        questions = if (reset) page.items else it.questions + page.items,
                        nextCursor = page.nextCursor,
                        hasMore = page.hasMore,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        message = error.message ?: "题目加载失败",
                    )
                }
            }
        }
    }

    fun loadMore() {
        loadQuestions(reset = false)
    }

    fun setFilter(filter: QuestionFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun setTag(tag: String?) {
        if (_state.value.selectedTag == tag) return
        // 选标签时回到“全部题型”，避免用户在一个旧的题型筛选下只看到少量结果。
        _state.update { it.copy(selectedTag = tag, filter = QuestionFilter.All) }
        loadQuestions(reset = true)
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadQuestions(reset = true)
        }
    }

    fun toggleSearch() {
        val current = _state.value
        if (current.searchVisible && current.query.isNotBlank()) {
            _state.update { it.copy(searchVisible = false, query = "") }
            loadQuestions(reset = true)
        } else {
            _state.update { it.copy(searchVisible = !current.searchVisible) }
        }
    }

    fun toggleTags() {
        _state.update { it.copy(tagsExpanded = !it.tagsExpanded) }
    }

    fun loadTags(cursor: String?) {
        if (_state.value.loadingTags) return
        _state.update { it.copy(loadingTags = true) }
        viewModelScope.launch {
            runCatching { repository.tags(cursor, TAG_PAGE_SIZE, keyword = "") }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loadingTags = false,
                            tags = (it.tags + page.items).distinctBy { tag -> tag.name },
                            tagNextCursor = page.nextCursor,
                            hasMoreTags = page.hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loadingTags = false, message = error.message ?: "标签加载失败")
                    }
                }
        }
    }

    fun loadMoreTags() {
        val current = _state.value
        if (!current.hasMoreTags || current.loadingTags) return
        loadTags(current.tagNextCursor)
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val TAG_PAGE_SIZE = 50
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
