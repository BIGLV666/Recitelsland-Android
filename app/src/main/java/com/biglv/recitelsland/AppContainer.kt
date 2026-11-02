package com.biglv.recitelsland

import android.content.Context
import com.biglv.recitelsland.core.avatar.AvatarCache
import com.biglv.recitelsland.core.settings.AppSettingsStore
import com.biglv.recitelsland.core.settings.ThemeController
import com.biglv.recitelsland.core.auth.AuthRepository
import com.biglv.recitelsland.core.auth.SessionController
import com.biglv.recitelsland.core.auth.SessionManager
import com.biglv.recitelsland.core.auth.SessionStore
import com.biglv.recitelsland.data.remote.ApiClient
import com.biglv.recitelsland.data.remote.ApiExecutor
import com.biglv.recitelsland.data.repo.AdminRepository
import com.biglv.recitelsland.data.repo.FeedbackRepository
import com.biglv.recitelsland.data.repo.ReciteRepository
import com.biglv.recitelsland.data.repo.RemoteReciteRepository

class AppContainer(context: Context) {
    private val sessionStore = SessionStore(context)
    val sessionManager = SessionManager()
    val themeController = ThemeController(AppSettingsStore(context))
    val avatarCache = AvatarCache(context)
    private val sessionController = SessionController(sessionStore, sessionManager)

    private val api = ApiClient.create { sessionManager.currentToken }
    private val executor = ApiExecutor(
        json = ApiClient.json,
        onUnauthorized = { sessionController.forceLogout() },
    )

    val authRepository = AuthRepository(
        api = api,
        controller = sessionController,
        manager = sessionManager,
        executor = executor,
    )

    val repository: ReciteRepository = RemoteReciteRepository(api, executor)
    val adminRepository = AdminRepository(api, executor)
    val feedbackRepository = FeedbackRepository(api, executor)
}
