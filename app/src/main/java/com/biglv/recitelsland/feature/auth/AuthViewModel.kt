package com.biglv.recitelsland.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biglv.recitelsland.core.auth.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { Login, Register }

data class AuthUiState(
    val mode: AuthMode = AuthMode.Login,
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val nickname: String = "",
    val code: String = "",
    val loading: Boolean = false,
    val sendingCode: Boolean = false,
    val countdown: Int = 0,
    val error: String? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    fun setEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun setUsername(value: String) = _state.update { it.copy(username = value, error = null) }
    fun setNickname(value: String) = _state.update { it.copy(nickname = value, error = null) }
    fun setCode(value: String) = _state.update { it.copy(code = value, error = null) }

    fun switchMode(mode: AuthMode) {
        _state.update { it.copy(mode = mode, error = null) }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun login() {
        val current = _state.value
        if (current.loading) return
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = "请输入邮箱和密码") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                authRepository.login(current.email, current.password)
            }.onSuccess {
                _state.update { it.copy(loading = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, error = error.message ?: "登录失败")
                }
            }
        }
    }

    fun register() {
        val current = _state.value
        if (current.loading) return
        if (current.username.isBlank() || current.email.isBlank() ||
            current.password.isBlank() || current.code.isBlank()
        ) {
            _state.update { it.copy(error = "请完整填写用户名、邮箱、密码和验证码") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                authRepository.register(
                    username = current.username,
                    nickname = current.nickname,
                    email = current.email,
                    password = current.password,
                    code = current.code,
                )
            }.onSuccess {
                _state.update { it.copy(loading = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, error = error.message ?: "注册失败")
                }
            }
        }
    }

    fun sendCode() {
        val current = _state.value
        if (current.sendingCode || current.countdown > 0) return
        if (current.email.isBlank()) {
            _state.update { it.copy(error = "请先填写邮箱") }
            return
        }
        _state.update { it.copy(sendingCode = true, error = null) }
        viewModelScope.launch {
            runCatching { authRepository.sendCode(current.email) }
                .onSuccess {
                    _state.update { it.copy(sendingCode = false, countdown = 60, error = null) }
                    startCountdown()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(sendingCode = false, error = error.message ?: "验证码发送失败")
                    }
                }
        }
    }

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
