package com.biglv.recitelsland.feature.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.data.model.ReviewItem
import com.biglv.recitelsland.ui.components.EmptyState
import com.biglv.recitelsland.ui.components.MarkdownText
import com.biglv.recitelsland.ui.components.TypeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: ReviewViewModel = viewModel(
        factory = simpleViewModelFactory { ReviewViewModel(app.container.repository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

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
                        Text("复习", style = MaterialTheme.typography.titleLarge)
                    }
                },
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
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                    icon = Icons.Outlined.EventAvailable,
                                    title = "今天没有待复习的题目",
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("review_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            item(key = "summary") {
                                ReviewSummaryCard(
                                    total = state.items.size,
                                    overdue = state.overdueCount,
                                )
                            }
                            item(key = "section_title") {
                                Text(
                                    text = "复习清单",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                                )
                            }
                            items(state.items, key = { it.reviewId }) { item ->
                                ReviewCard(
                                    item = item,
                                    onStart = { viewModel.openReview(item) },
                                )
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

    state.selected?.let { item ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSheet,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ReviewSheetContent(
                item = item,
                selectedAnswers = state.selectedAnswers,
                rating = state.rating,
                submitting = state.submitting,
                formError = state.formError,
                onToggleAnswer = viewModel::toggleAnswer,
                onRating = viewModel::setRating,
                onSubmit = viewModel::submit,
            )
        }
    }
}

@Composable
private fun ReviewSummaryCard(total: Int, overdue: Int) {
    val brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(brush)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今日待复习",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "道题",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (overdue > 0) "逾期 $overdue 题" else "暂无逾期",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "预计 ${(total * 3).coerceAtLeast(1)} 分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(item: ReviewItem, onStart: () -> Unit) {
    val dueColor = if (item.isOverdue) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_${item.reviewId}"),
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
                    color = dueColor,
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
                Text(
                    text = item.question.tags.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_review_${item.reviewId}"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text("开始复习")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewSheetContent(
    item: ReviewItem,
    selectedAnswers: List<String>,
    rating: Int,
    submitting: Boolean,
    formError: String?,
    onToggleAnswer: (String) -> Unit,
    onRating: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    val reading = item.question.type == QuestionType.Reading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Text(
            text = item.question.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(type = item.question.type)
            Spacer(Modifier.width(10.dp))
            Text(
                text = item.nextReviewAt.toDueLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = if (item.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))

        MarkdownText(content = item.question.context)

        if (!reading) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "选项",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            item.question.options.forEachIndexed { index, option ->
                val letter = ('A' + index).toString()
                val selected = letter in selectedAnswers
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_option_${item.reviewId}_$letter")
                        .clickable(enabled = !submitting) { onToggleAnswer(letter) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = letter, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(text = option, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (index != item.question.options.lastIndex) Spacer(Modifier.height(8.dp))
            }
        } else {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "这篇材料对你的难度如何？",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (0..5).forEach { value ->
                    FilterChip(
                        selected = rating == value,
                        onClick = { onRating(value) },
                        label = { Text("$value") },
                        enabled = !submitting,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "0 表示完全忘记，5 表示非常熟悉。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        formError?.let { error ->
            Spacer(Modifier.height(14.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onSubmit,
            enabled = !submitting && (reading && rating >= 0 || !reading && selectedAnswers.isNotEmpty()),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("review_submit"),
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("提交中")
            } else {
                Text("提交复习")
            }
        }
    }
}
