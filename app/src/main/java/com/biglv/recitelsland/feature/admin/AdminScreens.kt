package com.biglv.recitelsland.feature.admin

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.core.ui.toRecordLabel
import com.biglv.recitelsland.data.model.AdminStats
import com.biglv.recitelsland.data.model.AdminUser
import com.biglv.recitelsland.data.model.AdminUserDetail
import com.biglv.recitelsland.data.model.FailedRequest
import com.biglv.recitelsland.data.model.FeedbackItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenFeedbacks: () -> Unit,
    onOpenFailedRequests: () -> Unit,
    onOpenQuestions: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: AdminStatsViewModel = viewModel(
        factory = simpleViewModelFactory { AdminStatsViewModel(app.container.adminRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    AdminPage(title = "管理端", onBack = onBack, snackbar = snackbar) {
        if (state.loading) {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        } else {
            val stats = state.stats
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (stats != null) {
                    item(key = "stats") { AdminStatsCard(stats) }
                }
                item(key = "users") {
                    AdminNavRow("用户管理", "账号、角色、封禁", Icons.Outlined.Group, onOpenUsers)
                }
                item(key = "feedbacks") {
                    AdminNavRow("反馈管理", "查看和回复用户反馈", Icons.Outlined.Feedback, onOpenFeedbacks)
                }
                item(key = "failed") {
                    AdminNavRow("失败请求", "最近的 5xx 请求", Icons.Outlined.BugReport, onOpenFailedRequests)
                }
                item(key = "questions") {
                    AdminNavRow("题库管理", "新建、修改、删除、批量导入", Icons.AutoMirrored.Outlined.MenuBook, onOpenQuestions)
                }
            }
        }
    }
}

@Composable
private fun AdminStatsCard(stats: AdminStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("统计", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStat("用户", stats.users, Modifier.weight(1f))
                AdminStat("题目", stats.questions, Modifier.weight(1f))
                AdminStat("反馈", stats.feedbacks, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStat("作答", stats.answerRecords, Modifier.weight(1f))
                AdminStat("复习", stats.reviews, Modifier.weight(1f))
                AdminStat("待处理反馈", stats.pendingFeedbacks, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminStat(label: String, value: Long, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AdminNavRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: AdminUsersViewModel = viewModel(
        factory = simpleViewModelFactory { AdminUsersViewModel(app.container.adminRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<AdminUser?>(null) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    AdminPage(title = "用户管理", onBack = onBack, snackbar = snackbar) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.items, key = { it.id }) { user ->
                AdminUserCard(user = user, onClick = { viewModel.openDetail(user.id) })
            }
            if (state.canLoadMore) {
                item(key = "more") {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else TextButton(onClick = viewModel::loadMore) { Text("加载更多") }
                    }
                }
            }
        }
    }

    val detail = state.detail
    if (detail != null || state.detailLoading) {
        AlertDialog(
            onDismissRequest = viewModel::closeDetail,
            title = { Text("用户详情") },
            text = {
                if (state.detailLoading || detail == null) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                } else {
                    AdminUserDetailContent(detail)
                }
            },
            confirmButton = {
                if (detail != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(
                            enabled = !state.updating,
                            onClick = { viewModel.setRole(detail.user.id, !detail.user.isAdmin) },
                        ) {
                            Text(if (detail.user.isAdmin) "设为普通用户" else "设为管理员")
                        }
                        TextButton(
                            enabled = !state.updating && !detail.user.isAdmin,
                            onClick = { viewModel.setStatus(detail.user.id, !detail.user.isBanned) },
                        ) {
                            Text(if (detail.user.isBanned) "解封" else "封禁")
                        }
                        TextButton(
                            enabled = !state.updating && !detail.user.isAdmin,
                            onClick = { pendingDelete = detail.user },
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = viewModel::closeDetail) { Text("关闭") } },
        )
    }

    pendingDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除用户") },
            text = { Text("确定删除 ${user.nickname.ifBlank { user.email }} 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.deleteUser(user.id)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun AdminUserCard(user: AdminUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user.nickname.ifBlank { user.username },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (user.isAdmin) "管理员" else "普通用户",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                if (user.isBanned) "已封禁" else "正常",
                style = MaterialTheme.typography.labelSmall,
                color = if (user.isBanned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdminUserDetailContent(detail: AdminUserDetail) {
    Column {
        Text(detail.user.nickname.ifBlank { detail.user.username }, style = MaterialTheme.typography.titleMedium)
        Text(detail.user.email, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Text("角色：${if (detail.user.isAdmin) "管理员" else "普通用户"}")
        Text("状态：${if (detail.user.isBanned) "已封禁" else "正常"}")
        Text("作答记录：${detail.answerRecords}")
        Text("收藏：${detail.favorites}")
        Text("复习计划：${detail.reviews}")
        Text("复习记录：${detail.reviewRecords}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedbacksScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: AdminFeedbacksViewModel = viewModel(
        factory = simpleViewModelFactory { AdminFeedbacksViewModel(app.container.adminRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    AdminPage(title = "反馈管理", onBack = onBack, snackbar = snackbar) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("" to "全部", "pending" to "待处理", "replied" to "已回复").forEach {
                    FilterChip(
                        selected = state.status == it.first,
                        onClick = { viewModel.setStatus(it.first) },
                        label = { Text(it.second) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    AdminFeedbackCard(item = item, onReply = { viewModel.openReply(item) })
                }
                if (state.canLoadMore) {
                    item(key = "more") {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            else TextButton(onClick = viewModel::loadMore) { Text("加载更多") }
                        }
                    }
                }
            }
        }
    }

    state.replying?.let { item ->
        var reply by remember(item.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::closeReply,
            title = { Text("回复反馈") },
            text = {
                Column {
                    Text(item.content, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        label = { Text("回复内容") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.sending,
                    onClick = { viewModel.reply(reply) },
                ) { Text("提交回复") }
            },
            dismissButton = { TextButton(onClick = viewModel::closeReply) { Text("取消") } },
        )
    }
}

@Composable
private fun AdminFeedbackCard(item: FeedbackItem, onReply: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.userNickname.ifBlank { item.userEmail }.ifBlank { "用户 ${item.userId}" },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (item.isPending) "待处理" else "已回复",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isPending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(item.content, style = MaterialTheme.typography.bodyMedium)
            if (item.adminReply.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("回复：${item.adminReply}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.createdAt.toRecordLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.weight(1f))
                if (item.isPending) {
                    TextButton(onClick = onReply) { Text("回复") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFailedRequestsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: AdminFailedViewModel = viewModel(
        factory = simpleViewModelFactory { AdminFailedViewModel(app.container.adminRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    AdminPage(title = "失败请求", onBack = onBack, snackbar = snackbar) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.items, key = { it.requestId }) { item ->
                FailedRequestCard(item)
            }
            if (state.canLoadMore) {
                item(key = "more") {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else TextButton(onClick = viewModel::loadMore) { Text("加载更多") }
                    }
                }
            }
            if (state.items.isEmpty() && !state.loading) {
                item(key = "empty") {
                    Text("暂无失败请求", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FailedRequestCard(item: FailedRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text("${item.method} ${item.status}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.weight(1f))
                Text(item.time.toRecordLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(4.dp))
            Text(item.path, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text(item.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${item.latencyMs} ms · ${item.ip}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPage(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
            content()
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
