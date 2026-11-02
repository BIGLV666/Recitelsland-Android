package com.biglv.recitelsland.feature.feedback

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.core.ui.toRecordLabel
import com.biglv.recitelsland.data.model.FeedbackItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: FeedbackViewModel = viewModel(
        factory = simpleViewModelFactory { FeedbackViewModel(app.container.feedbackRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("意见反馈", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "form") {
                    FeedbackForm(state = state, viewModel = viewModel)
                }
                item(key = "mine_title") {
                    Text(
                        text = "我的反馈",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (state.items.isEmpty() && !state.loading) {
                    item(key = "empty") {
                        Text(
                            text = "还没有反馈记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(state.items, key = { it.id }) { item ->
                    FeedbackCard(item = item)
                }
                if (state.canLoadMore) {
                    item(key = "more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                TextButton(onClick = viewModel::loadMore) { Text("加载更多") }
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
}

@Composable
private fun FeedbackForm(state: FeedbackUiState, viewModel: FeedbackViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("bug" to "问题反馈", "feature" to "功能建议", "other" to "其他").forEach {
                    FilterChip(
                        selected = state.type == it.first,
                        onClick = { viewModel.setType(it.first) },
                        label = { Text(it.second) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::setContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_content"),
                label = { Text("反馈内容") },
                minLines = 5,
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.contact,
                onValueChange = viewModel::setContact,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("联系方式（可选）") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.appVersion,
                onValueChange = viewModel::setAppVersion,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("App 版本（自动填入，可修改）") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.device,
                onValueChange = viewModel::setDevice,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("设备信息（自动填入，可修改）") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = viewModel::submit,
                enabled = !state.sending,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_submit"),
            ) {
                if (state.sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("提交反馈")
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(item: FeedbackItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Feedback,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = typeLabel(item.type),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (item.isPending) "待处理" else "已回复",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isPending) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.adminReply.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "回复：${item.adminReply}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.createdAt.toRecordLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun typeLabel(type: String): String = when (type) {
    "bug" -> "问题反馈"
    "feature" -> "功能建议"
    else -> "其他"
}
