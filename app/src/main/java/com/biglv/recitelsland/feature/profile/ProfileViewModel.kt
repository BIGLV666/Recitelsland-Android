package com.biglv.recitelsland.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.core.auth.AuthRepository
import com.biglv.recitelsland.core.avatar.AvatarCache
import com.biglv.recitelsland.data.model.AnswerFilter
import com.biglv.recitelsland.data.model.UserProfile
import com.biglv.recitelsland.data.repo.ReciteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ProfileUiState(
    val loading: Boolean = true,
    val user: UserProfile? = null,
    val avatarFile: File? = null,
    val todayReviewCount: Int = 0,
    val recordCount: Int = 0,
    val loggingOut: Boolean = false,
    val message: String? = null,
)

class ProfileViewModel(
    private val repository: ReciteRepository,
    private val authRepository: AuthRepository,
    private val avatarCache: AvatarCache,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { repository.currentUser() }
                .onSuccess { user ->
                    val reviews = runCatching { repository.todayReviews() }.getOrDefault(emptyList())
                    val records = runCatching {
                        repository.answerRecords(cursor = null, limit = 20, filter = AnswerFilter.All).items
                    }.getOrDefault(emptyList())
                    _state.update {
                        it.copy(
                            loading = false,
                            user = user,
                            todayReviewCount = reviews.size,
                            recordCount = records.size,
                        )
                    }
                    val avatar = avatarCache.ensure(user.id, user.avatar)
                    _state.update { it.copy(avatarFile = avatar) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, message = error.message ?: "用户信息加载失败")
                    }
                }
        }
    }

    fun refreshUserAndAvatar() {
        viewModelScope.launch {
            runCatching { repository.currentUser() }
                .onSuccess { user ->
                    _state.update { it.copy(user = user) }
                    val avatar = avatarCache.ensure(user.id, user.avatar)
                    _state.update { it.copy(avatarFile = avatar) }
                }
        }
    }

    fun logout() {
        if (_state.value.loggingOut) return
        _state.update { it.copy(loggingOut = true) }
        viewModelScope.launch {
            authRepository.logout()
            _state.update { it.copy(loggingOut = false) }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
