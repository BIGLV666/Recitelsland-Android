package com.biglv.recitelsland

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.biglv.recitelsland.core.settings.ThemeMode
import com.biglv.recitelsland.navigation.ReciteRoot
import com.biglv.recitelsland.ui.theme.ReciteTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var handledGithubCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as ReciteApplication
            val themeMode by app.container.themeController.mode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
            ReciteTheme(darkTheme = darkTheme) {
                ReciteRoot()
            }
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "recitelsland" || data.host != "auth") return
        val code = data.getQueryParameter("code")?.takeIf { it.isNotBlank() } ?: return
        if (code == handledGithubCode) return
        handledGithubCode = code

        lifecycleScope.launch {
            runCatching {
                (application as ReciteApplication).container.authRepository.exchangeGithubCode(code)
            }.onFailure { error ->
                Toast.makeText(
                    this@MainActivity,
                    error.message ?: "GitHub 登录失败，请重试",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}
