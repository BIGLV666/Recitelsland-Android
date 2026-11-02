package com.biglv.recitelsland.core.auth

import com.biglv.recitelsland.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val user: UserProfile) : SessionState
}

/**
 * 会话内存状态。OkHttp 拦截器同步读取 token，所以 token 用 @Volatile 暴露。
 */
class SessionManager {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    @Volatile
    var currentToken: String? = null
        private set

    fun markLoggedIn(token: String, user: UserProfile) {
        currentToken = token
        _state.value = SessionState.LoggedIn(user)
    }

    fun updateUser(user: UserProfile) {
        if (_state.value is SessionState.LoggedIn) {
            _state.value = SessionState.LoggedIn(user)
        }
    }

    fun markLoggedOut() {
        currentToken = null
        _state.value = SessionState.LoggedOut
    }
}
