package com.biglv.recitelsland.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.biglv.recitelsland.core.settings.ThemeMode
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.data.model.UserProfile
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenAbout: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenReviewPlans: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: ProfileViewModel = viewModel(
        factory = simpleViewModelFactory {
            ProfileViewModel(
                app.container.repository,
                app.container.authRepository,
                app.container.avatarCache,
            )
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by app.container.themeController.mode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showThemeDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshUserAndAvatar()
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("我的", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            when {
                state.loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                state.user == null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "用户信息加载失败",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                else -> {
                    ProfileContent(
                        user = state.user!!,
                        avatarFile = state.avatarFile,
                        todayReviewCount = state.todayReviewCount,
                        recordCount = state.recordCount,
                        loggingOut = state.loggingOut,
                        onOpenAbout = onOpenAbout,
                        onOpenFavorites = onOpenFavorites,
                        onOpenReviewPlans = onOpenReviewPlans,
                        onOpenEditProfile = onOpenEditProfile,
                        onOpenFeedback = onOpenFeedback,
                        onOpenAdmin = onOpenAdmin,
                        onThemeClick = { showThemeDialog = true },
                        onLogout = viewModel::logout,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            current = themeMode,
            onSelect = {
                app.container.themeController.setMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
}

@Composable
private fun ProfileContent(
    user: UserProfile,
    avatarFile: File?,
    todayReviewCount: Int,
    recordCount: Int,
    loggingOut: Boolean,
    onOpenAbout: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenReviewPlans: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenAdmin: () -> Unit,
    onThemeClick: () -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "user") {
            UserCard(
                user = user,
                avatarFile = avatarFile,
                onClick = onOpenEditProfile,
            )
        }
        item(key = "stats") {
            StatsCard(
                learned = user.learnedQuestionTotal,
                todayReview = todayReviewCount,
                records = recordCount,
            )
        }
        item(key = "settings_title") {
            Text(
                text = "账号与设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
        item(key = "settings") {
            SettingsCard(
                onOpenAbout = onOpenAbout,
                onOpenFavorites = onOpenFavorites,
                onOpenReviewPlans = onOpenReviewPlans,
                onOpenEditProfile = onOpenEditProfile,
                onOpenFeedback = onOpenFeedback,
                onOpenAdmin = onOpenAdmin,
                isAdmin = user.isAdmin,
                onThemeClick = onThemeClick,
            )
        }
        item(key = "logout") {
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logout"),
                enabled = !loggingOut,
            ) {
                if (loggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("退出登录")
            }
        }
        item(key = "footer") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "背诵岛 v0.4.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "社区协议与隐私说明见“关于背诵岛”",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("退出后需要重新登录才能继续使用题库、记录和复习功能。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun UserCard(
    user: UserProfile,
    avatarFile: File?,
    onClick: () -> Unit,
) {
    val brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
        ),
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(brush),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarFile != null) {
                    SubcomposeAsyncImage(
                        model = avatarFile,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { AvatarInitial(user) },
                        error = { AvatarInitial(user) },
                    )
                } else {
                    AvatarInitial(user)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.nickname.ifBlank { user.username },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        text = user.role,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarInitial(user: UserProfile) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = user.nickname.trim().firstOrNull()?.toString() ?: "岛",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun StatsCard(learned: Long, todayReview: Int, records: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileStat(value = learned.toString(), label = "累计学习", modifier = Modifier.weight(1f))
            StatDivider()
            ProfileStat(value = todayReview.toString(), label = "今日复习", modifier = Modifier.weight(1f))
            StatDivider()
            ProfileStat(value = records.toString(), label = "近期记录", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun SettingsCard(
    onOpenAbout: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenReviewPlans: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenAdmin: () -> Unit,
    isAdmin: Boolean,
    onThemeClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            SettingRow(
                title = "编辑资料",
                testTag = "setting_edit_profile",
                onClick = onOpenEditProfile,
            )
            SettingsDivider()
            SettingRow(
                title = "我的收藏",
                testTag = "setting_favorites",
                onClick = onOpenFavorites,
            )
            SettingsDivider()
            SettingRow(
                title = "复习计划",
                testTag = "setting_review_plans",
                onClick = onOpenReviewPlans,
            )
            SettingsDivider()
            SettingRow(
                title = "意见反馈",
                testTag = "setting_feedback",
                onClick = onOpenFeedback,
            )
            SettingsDivider()
            if (isAdmin) {
                SettingRow(
                    title = "管理端",
                    testTag = "setting_admin",
                    onClick = onOpenAdmin,
                )
                SettingsDivider()
            }
            SettingRow(
                title = "外观",
                testTag = "setting_theme",
                onClick = onThemeClick,
            )
            SettingsDivider()
            SettingRow(
                title = "关于背诵岛",
                testTag = "setting_about",
                onClick = onOpenAbout,
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    testTag: String,
    onClick: () -> Unit,
    subtitle: String = "",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}


@Composable
private fun ThemeModeDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("外观") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = { onSelect(mode) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
