package com.biglv.recitelsland.feature.reviewplans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.core.ui.toDueLabel
import com.biglv.recitelsland.data.model.ReviewItem
import com.biglv.recitelsland.ui.components.EmptyState
import com.biglv.recitelsland.ui.components.TagText
import com.biglv.recitelsland.ui.components.TypeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPlansScreen(
    onBack: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: ReviewPlansViewModel = viewModel(
        factory = simpleViewModelFactory { ReviewPlansViewModel(app.container.repository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRemove by remember { mutableStateOf<ReviewItem?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
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
                title = { Text("复习计划", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.loading -> {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    state.items.isEmpty() -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                EmptyState(
                                    icon = Icons.Outlined.EventRepeat,
                                    title = "复习计划还是空的",
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("review_plans_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.items, key = { it.reviewId }) { item ->
                                ReviewPlanCard(
                                    item = item,
                                    onOpen = { onOpenQuestion(item.question.id) },
                                    onRemove = { pendingRemove = item },
                                )
                            }
                            if (state.hasMore) {
                                item(key = "load_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (state.loadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            TextButton(onClick = viewModel::loadMore) {
                                                Text("加载更多")
                                            }
                                        }
                                    }
                                }
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

    pendingRemove?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("移出复习计划") },
            text = { Text("确定不再复习“${item.question.title}”吗？已有的历史复习记录不会删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemove = null
                        viewModel.remove(item.reviewId)
                    },
                ) {
                    Text("移出")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ReviewPlanCard(
    item: ReviewItem,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_plan_${item.reviewId}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(type = item.question.type)
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.nextReviewAt.toDueLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isOverdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.question.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "连续答对 ${item.repetitions} 次 · 当前间隔 ${item.intervalDays} 天",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.question.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.question.tags.take(3).forEach { TagText(tag = it) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("查看题目")
                }
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("remove_review_${item.reviewId}"),
                ) {
                    Text("移出计划")
                }
            }
        }
    }
}
