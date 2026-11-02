package com.biglv.recitelsland.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.auth.SessionState
import com.biglv.recitelsland.feature.auth.AuthScreen

@Composable
fun ReciteRoot() {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val session by app.container.sessionManager.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        app.container.authRepository.restore()
    }

    when (session) {
        SessionState.Loading -> SplashScreen()
        SessionState.LoggedOut -> AuthScreen(authRepository = app.container.authRepository)
        is SessionState.LoggedIn -> ReciteApp()
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp),
            )
            Text(
                text = "背诵岛",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
