package com.biglv.recitelsland.feature.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.ui.readAvatarBytes
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.data.model.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val context = LocalContext.current
    val viewModel: EditProfileViewModel = viewModel(
        factory = simpleViewModelFactory {
            EditProfileViewModel(app.container.authRepository, app.container.avatarCache)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteAccount by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val data = readAvatarBytes(context, uri)
                if (data == null) {
                    Toast.makeText(context, "无法读取图片", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.uploadAvatar(data.first, data.second)
                }
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("编辑资料", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            if (state.loading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                val user = state.user
                if (user == null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "资料加载失败",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        AvatarSection(
                            user = user,
                            avatarFile = state.avatarFile,
                            uploading = state.avatarUploading,
                            onPick = { picker.launch("image/*") },
                            onDelete = viewModel::deleteAvatar,
                        )

                        SectionCard(title = "昵称") {
                            OutlinedTextField(
                                value = state.nickname,
                                onValueChange = viewModel::setNickname,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_nickname"),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = viewModel::saveNickname,
                                enabled = !state.savingNickname,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (state.savingNickname) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Text("保存昵称")
                                }
                            }
                        }

                        SectionCard(title = "邮箱") {
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = viewModel::setEmail,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("新邮箱") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = MaterialTheme.shapes.medium,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = state.code,
                                    onValueChange = viewModel::setCode,
                                    modifier = Modifier.weight(1f),
                                    label = { Text("验证码") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                Spacer(Modifier.width(10.dp))
                                OutlinedButton(
                                    onClick = viewModel::sendBindEmailCode,
                                    enabled = !state.sendingCode && state.countdown == 0,
                                    modifier = Modifier.height(56.dp),
                                ) {
                                    Text(
                                        when {
                                            state.sendingCode -> "发送中"
                                            state.countdown > 0 -> "${state.countdown}s"
                                            else -> "获取验证码"
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = viewModel::bindEmail,
                                enabled = !state.bindingEmail,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (state.bindingEmail) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Text("更换邮箱")
                                }
                            }
                        }

                        SectionCard(title = "账号") {
                            TextButton(
                                onClick = { showDeleteAccount = true },
                                enabled = !state.deletingAccount,
                            ) {
                                Text("注销账号", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
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

    if (showDeleteAccount) {
        DeleteAccountDialog(
            deleting = state.deletingAccount,
            onDismiss = { showDeleteAccount = false },
            onConfirm = { password ->
                showDeleteAccount = false
                viewModel.deleteAccount(password)
            },
        )
    }
}

@Composable
private fun AvatarSection(
    user: UserProfile,
    avatarFile: java.io.File?,
    uploading: Boolean,
    onPick: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(enabled = !uploading, onClick = onPick),
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
            if (uploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPick, enabled = !uploading) {
                Text("更换头像")
            }
            if (avatarFile != null || user.avatar.isNotBlank()) {
                TextButton(onClick = onDelete, enabled = !uploading) {
                    Text("删除头像")
                }
            }
        }
    }
}

@Composable
private fun AvatarInitial(user: UserProfile) {
    Text(
        text = user.nickname.trim().firstOrNull()?.toString() ?: "岛",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    deleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("注销账号") },
        text = {
            Column {
                Text("注销后账号和关联数据将被删除，且无法恢复。邮箱账号需要输入密码。")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = !deleting,
            ) {
                Text("注销", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
