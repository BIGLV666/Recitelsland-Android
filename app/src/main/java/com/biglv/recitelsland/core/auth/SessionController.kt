package com.biglv.recitelsland.core.auth

import com.biglv.recitelsland.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 会话编排：登录、注册、GitHub 换码、恢复、退出。
 */
class SessionController(
    private val store: SessionStore,
    private val manager: SessionManager,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    suspend fun readStored(): StoredSession? = store.read()

    suspend fun save(token: String, user: UserProfile) {
        manager.markLoggedIn(token, user)
        store.save(token, user)
    }

    suspend fun updateUser(user: UserProfile) {
        manager.updateUser(user)
        val token = manager.currentToken ?: return
        store.save(token, user)
    }

    suspend fun clear() {
        manager.markLoggedOut()
        store.clear()
    }

    /**
     * 401 时由网络层触发。DataStore 清理放到应用级协程，避免阻塞响应处理。
     */
    fun forceLogout() {
        manager.markLoggedOut()
        appScope.launch { store.clear() }
    }
}
