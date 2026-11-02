package com.biglv.recitelsland.feature.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biglv.recitelsland.core.auth.AuthRepository
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.data.remote.ApiConfig
import com.biglv.recitelsland.feature.legal.LegalDoc
import com.biglv.recitelsland.feature.legal.LegalDocuments
import com.biglv.recitelsland.ui.components.MarkdownText

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: AuthViewModel = viewModel(
        factory = simpleViewModelFactory { AuthViewModel(authRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var legalDoc by remember { mutableStateOf<LegalDoc?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "背诵岛",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(26.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AuthModeSwitch(
                        mode = state.mode,
                        onModeChange = viewModel::switchMode,
                    )
                    Spacer(Modifier.height(18.dp))

                    if (state.mode == AuthMode.Register) {
                        OutlinedTextField(
                            value = state.username,
                            onValueChange = viewModel::setUsername,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("用户名") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.nickname,
                            onValueChange = viewModel::setNickname,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("昵称（可选）") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::setEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email"),
                        label = { Text("邮箱") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = MaterialTheme.shapes.medium,
                    )

                    if (state.mode == AuthMode.Register) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = state.code,
                                onValueChange = viewModel::setCode,
                                modifier = Modifier.weight(1f),
                                label = { Text("邮箱验证码") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.medium,
                            )
                            Spacer(Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = viewModel::sendCode,
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
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::setPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password"),
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                    )
                    if (state.mode == AuthMode.Register) {
                        Text(
                            text = "密码至少 8 位，需包含大写字母、小写字母和数字",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }

                    state.error?.let { error ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (state.mode == AuthMode.Login) viewModel.login() else viewModel.register()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit"),
                        enabled = !state.loading,
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(if (state.mode == AuthMode.Login) "登录" else "注册并登录")
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "或",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(ApiConfig.GITHUB_LOGIN_URL)),
                                )
                            }.onFailure {
                                Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text("使用 GitHub 账号登录")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "登录即表示你已阅读并同意以下条款",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { legalDoc = LegalDocuments.terms }) {
                    Text("用户协议")
                }
                Text(
                    text = "和",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                TextButton(onClick = { legalDoc = LegalDocuments.privacy }) {
                    Text("隐私保护")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    legalDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { legalDoc = null },
            title = { Text(doc.title) },
            text = {
                Box(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    MarkdownText(content = doc.content)
                }
            },
            confirmButton = {
                TextButton(onClick = { legalDoc = null }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun AuthModeSwitch(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(AuthMode.Login to "登录", AuthMode.Register to "注册").forEach { (item, label) ->
            val selected = mode == item
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onModeChange(item) },
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }
        }
    }
}
