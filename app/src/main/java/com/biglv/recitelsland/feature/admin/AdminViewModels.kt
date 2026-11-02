package com.biglv.recitelsland.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.data.model.AdminStats
import com.biglv.recitelsland.data.model.AdminUser
import com.biglv.recitelsland.data.model.AdminUserDetail
import com.biglv.recitelsland.data.model.FailedRequest
import com.biglv.recitelsland.data.model.FeedbackItem
import com.biglv.recitelsland.data.model.ImportQuestionResult
import com.biglv.recitelsland.data.repo.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminStatsUiState(
    val loading: Boolean = true,
    val stats: AdminStats? = null,
    val message: String? = null,
)

class AdminStatsViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminStatsUiState())
    val state: StateFlow<AdminStatsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { repository.stats() }
                .onSuccess { stats -> _state.update { it.copy(loading = false, stats = stats) } }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, message = error.message ?: "统计加载失败") }
                }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

data class AdminUsersUiState(
    val loading: Boolean = false,
    val items: List<AdminUser> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val detail: AdminUserDetail? = null,
    val detailLoading: Boolean = false,
    val updating: Boolean = false,
    val message: String? = null,
) {
    val canLoadMore: Boolean get() = items.size < total
}

class AdminUsersViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminUsersUiState())
    val state: StateFlow<AdminUsersUiState> = _state.asStateFlow()

    init { load(reset = true) }

    fun load(reset: Boolean) {
        val current = _state.value
        if (current.loading || !reset && !current.canLoadMore) return
        val page = if (reset) 1 else current.page + 1
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { repository.users(page, current.limit) }
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
                    _state.update { it.copy(loading = false, message = error.message ?: "用户加载失败") }
                }
        }
    }

    fun loadMore() = load(reset = false)

    fun openDetail(id: Long) {
        _state.update { it.copy(detail = null, detailLoading = true) }
        viewModelScope.launch {
            runCatching { repository.userDetail(id) }
                .onSuccess { detail -> _state.update { it.copy(detail = detail, detailLoading = false) } }
                .onFailure { error ->
                    _state.update { it.copy(detailLoading = false, message = error.message ?: "用户详情加载失败") }
                }
        }
    }

    fun closeDetail() = _state.update { it.copy(detail = null, detailLoading = false) }

    fun setRole(id: Long, admin: Boolean) = action {
        repository.updateRole(id, admin)
    }

    fun setStatus(id: Long, banned: Boolean) = action {
        repository.updateStatus(id, banned)
    }

    fun deleteUser(id: Long) = action {
        repository.deleteUser(id)
        _state.update { it.copy(detail = null) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun action(block: suspend () -> Unit) {
        if (_state.value.updating) return
        _state.update { it.copy(updating = true) }
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess {
                    _state.update { it.copy(updating = false, message = "操作成功") }
                    load(reset = true)
                    _state.value.detail?.let { openDetail(it.user.id) }
                }
                .onFailure { error ->
                    _state.update { it.copy(updating = false, message = error.message ?: "操作失败") }
                }
        }
    }
}

data class AdminFeedbacksUiState(
    val loading: Boolean = false,
    val items: List<FeedbackItem> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val status: String = "",
    val replying: FeedbackItem? = null,
    val sending: Boolean = false,
    val message: String? = null,
) {
    val canLoadMore: Boolean get() = items.size < total
}

class AdminFeedbacksViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminFeedbacksUiState())
    val state: StateFlow<AdminFeedbacksUiState> = _state.asStateFlow()

    init { load(reset = true) }

    fun setStatus(status: String) {
        if (_state.value.status == status) return
        _state.update { it.copy(status = status) }
        load(reset = true)
    }

    fun load(reset: Boolean) {
        val current = _state.value
        if (current.loading || !reset && !current.canLoadMore) return
        val page = if (reset) 1 else current.page + 1
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { repository.feedbacks(page, current.limit, current.status) }
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
                    _state.update { it.copy(loading = false, message = error.message ?: "反馈加载失败") }
                }
        }
    }

    fun loadMore() = load(reset = false)
    fun openReply(item: FeedbackItem) = _state.update { it.copy(replying = item) }
    fun closeReply() = _state.update { it.copy(replying = null) }

    fun reply(text: String) {
        val current = _state.value
        val item = current.replying ?: return
        if (current.sending) return
        if (text.isBlank()) {
            _state.update { it.copy(message = "回复内容不能为空") }
            return
        }
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            runCatching { repository.replyFeedback(item.id, text.trim()) }
                .onSuccess { updated ->
                    val merged = updated.copy(
                        userId = item.userId,
                        userNickname = item.userNickname,
                        userEmail = item.userEmail,
                    )
                    _state.update {
                        it.copy(
                            sending = false,
                            replying = null,
                            items = it.items.map { row -> if (row.id == merged.id) merged else row },
                            message = "已回复",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(sending = false, message = error.message ?: "回复失败") }
                }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

data class AdminFailedUiState(
    val loading: Boolean = false,
    val items: List<FailedRequest> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val message: String? = null,
) {
    val canLoadMore: Boolean get() = items.size < total
}

class AdminFailedViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminFailedUiState())
    val state: StateFlow<AdminFailedUiState> = _state.asStateFlow()

    init { load(reset = true) }

    fun load(reset: Boolean) {
        val current = _state.value
        if (current.loading || !reset && !current.canLoadMore) return
        val page = if (reset) 1 else current.page + 1
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { repository.failedRequests(page, current.limit) }
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
                    _state.update { it.copy(loading = false, message = error.message ?: "失败请求加载失败") }
                }
        }
    }

    fun loadMore() = load(reset = false)
    fun clearMessage() = _state.update { it.copy(message = null) }
}

data class AdminQuestionsUiState(
    val loading: Boolean = false,
    val items: List<com.biglv.recitelsland.data.model.Question> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val message: String? = null,
)

class AdminQuestionsViewModel(
    private val repository: com.biglv.recitelsland.data.repo.ReciteRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminQuestionsUiState())
    val state: StateFlow<AdminQuestionsUiState> = _state.asStateFlow()

    init { load(reset = true) }

    fun load(reset: Boolean) {
        val current = _state.value
        if (reset && current.loading) return
        if (!reset && (current.loadingMore || !current.hasMore)) return
        val cursor = if (reset) null else current.nextCursor
        _state.update {
            if (reset) it.copy(loading = true, items = emptyList(), nextCursor = null, hasMore = false)
            else it.copy(loadingMore = true)
        }
        viewModelScope.launch {
            runCatching { repository.questions(cursor, 20, null, "") }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loading = false,
                            loadingMore = false,
                            items = if (reset) page.items else it.items + page.items,
                            nextCursor = page.nextCursor,
                            hasMore = page.hasMore,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, loadingMore = false, message = error.message ?: "题目加载失败") }
                }
        }
    }

    fun loadMore() = load(reset = false)

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching { adminRepository.deleteQuestion(id) }
                .onSuccess {
                    _state.update {
                        it.copy(items = it.items.filterNot { q -> q.id == id }, message = "题目已删除")
                    }
                }
                .onFailure { error -> _state.update { it.copy(message = error.message ?: "删除失败") } }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

class ImportQuestionsViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _result = MutableStateFlow<ImportQuestionResult?>(null)
    val result: StateFlow<ImportQuestionResult?> = _result.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setText(value: String) { _text.value = value }
    fun clearMessage() { _message.value = null }

    fun submit() {
        if (_loading.value) return
        val raw = _text.value.trim()
        if (raw.isEmpty()) {
            _message.value = "请粘贴题目 JSON"
            return
        }
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                val json = com.biglv.recitelsland.data.remote.ApiClient.json
                val request = if (raw.startsWith("[")) {
                    val list = json.decodeFromString<List<com.biglv.recitelsland.data.remote.CreateQuestionRequest>>(raw)
                    com.biglv.recitelsland.data.remote.ImportQuestionsRequest(list)
                } else {
                    json.decodeFromString<com.biglv.recitelsland.data.remote.ImportQuestionsRequest>(raw)
                }
                repository.importQuestions(request.questions)
            }.onSuccess { result ->
                _loading.value = false
                _result.value = result
                _message.value = "导入完成"
            }.onFailure { error ->
                _loading.value = false
                _message.value = error.message ?: "导入失败"
            }
        }
    }
}
