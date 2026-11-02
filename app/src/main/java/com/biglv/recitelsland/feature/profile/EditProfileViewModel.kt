package com.biglv.recitelsland.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.core.auth.AuthRepository
import com.biglv.recitelsland.core.avatar.AvatarCache
import com.biglv.recitelsland.data.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EditProfileUiState(
    val loading: Boolean = true,
    val user: UserProfile? = null,
    val avatarFile: File? = null,
    val avatarUploading: Boolean = false,
    val nickname: String = "",
    val savingNickname: Boolean = false,
    val email: String = "",
    val code: String = "",
    val sendingCode: Boolean = false,
    val bindingEmail: Boolean = false,
    val countdown: Int = 0,
    val deletingAccount: Boolean = false,
    val message: String? = null,
)

class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val avatarCache: AvatarCache,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { authRepository.refreshUser() }
                .onSuccess { user ->
                    val avatar = avatarCache.ensure(user.id, user.avatar)
                    _state.update {
                        it.copy(
                            loading = false,
                            user = user,
                            avatarFile = avatar,
                            nickname = user.nickname,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, message = error.message ?: "资料加载失败")
                    }
                }
        }
    }

    fun setNickname(value: String) = _state.update { it.copy(nickname = value) }
    fun setEmail(value: String) = _state.update { it.copy(email = value) }
    fun setCode(value: String) = _state.update { it.copy(code = value) }

    fun saveNickname() {
        val current = _state.value
        val nickname = current.nickname.trim()
        if (current.savingNickname) return
        if (nickname.isEmpty()) {
            _state.update { it.copy(message = "昵称不能为空") }
            return
        }
        _state.update { it.copy(savingNickname = true) }
        viewModelScope.launch {
            runCatching { authRepository.updateNickname(nickname) }
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            savingNickname = false,
                            user = user,
                            nickname = user.nickname,
                            message = "昵称已更新",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(savingNickname = false, message = error.message ?: "昵称更新失败")
                    }
                }
        }
    }

    fun sendBindEmailCode() {
        val current = _state.value
        if (current.sendingCode || current.countdown > 0) return
        val email = current.email.trim()
        if (email.isEmpty()) {
            _state.update { it.copy(message = "请先填写新邮箱") }
            return
        }
        _state.update { it.copy(sendingCode = true) }
        viewModelScope.launch {
            runCatching { authRepository.sendBindEmailCode(email) }
                .onSuccess {
                    _state.update {
                        it.copy(sendingCode = false, countdown = 60, message = "验证码已发送")
                    }
                    startCountdown()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(sendingCode = false, message = error.message ?: "验证码发送失败")
                    }
                }
        }
    }

    fun bindEmail() {
        val current = _state.value
        if (current.bindingEmail) return
        val email = current.email.trim()
        val code = current.code.trim()
        if (email.isEmpty() || code.isEmpty()) {
            _state.update { it.copy(message = "请填写新邮箱和验证码") }
            return
        }
        _state.update { it.copy(bindingEmail = true) }
        viewModelScope.launch {
            runCatching { authRepository.bindEmail(email, code) }
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            bindingEmail = false,
                            user = user,
                            email = "",
                            code = "",
                            message = "邮箱已更新",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(bindingEmail = false, message = error.message ?: "邮箱更新失败")
                    }
                }
        }
    }

    fun uploadAvatar(bytes: ByteArray, contentType: String) {
        val current = _state.value
        val user = current.user ?: return
        if (current.avatarUploading) return
        _state.update { it.copy(avatarUploading = true) }
        viewModelScope.launch {
            runCatching { authRepository.uploadAvatar(bytes, contentType) }
                .onSuccess { updated ->
                    val file = avatarCache.save(
                        userId = updated.id,
                        remoteUrl = updated.avatar,
                        bytes = bytes,
                    )
                    _state.update {
                        it.copy(
                            avatarUploading = false,
                            user = updated,
                            avatarFile = file,
                            message = "头像已更新",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(avatarUploading = false, message = error.message ?: "头像上传失败")
                    }
                }
        }
    }

    fun deleteAvatar() {
        val current = _state.value
        val user = current.user ?: return
        if (current.avatarUploading) return
        _state.update { it.copy(avatarUploading = true) }
        viewModelScope.launch {
            runCatching { authRepository.deleteAvatar() }
                .onSuccess { updated ->
                    avatarCache.delete(user.id)
                    _state.update {
                        it.copy(
                            avatarUploading = false,
                            user = updated,
                            avatarFile = null,
                            message = "头像已删除",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(avatarUploading = false, message = error.message ?: "头像删除失败")
                    }
                }
        }
    }

    fun deleteAccount(password: String) {
        if (_state.value.deletingAccount) return
        _state.update { it.copy(deletingAccount = true) }
        viewModelScope.launch {
            runCatching { authRepository.deleteAccount(password) }
                .onSuccess {
                    avatarCache.delete(_state.value.user?.id ?: 0)
                    _state.update { it.copy(deletingAccount = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(deletingAccount = false, message = error.message ?: "注销失败")
                    }
                }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_state.value.countdown > 0) {
                delay(1000)
                _state.update { it.copy(countdown = (it.countdown - 1).coerceAtLeast(0)) }
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
