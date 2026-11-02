package com.biglv.recitelsland.feature.records

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.biglv.recitelsland.core.ui.toRecordLabel
import com.biglv.recitelsland.data.model.AnswerFilter
import com.biglv.recitelsland.data.model.AnswerRecord
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.ui.components.EmptyState
import com.biglv.recitelsland.ui.components.MarkdownText
import com.biglv.recitelsland.ui.components.ResultBadge
import com.biglv.recitelsland.ui.components.TypeBadge
import com.biglv.recitelsland.ui.theme.LocalSemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: RecordsViewModel = viewModel(
        factory = simpleViewModelFactory { RecordsViewModel(app.container.repository) },
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
                        Text("记录", style = MaterialTheme.typography.titleLarge)
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

                    state.records.isEmpty() -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                EmptyState(
                                    icon = Icons.Outlined.History,
                                    title = if (state.filter == AnswerFilter.All) {
                                        "还没有作答记录"
                                    } else {
                                        "这个筛选下没有记录"
                                    },
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                    }

                    else -> {
                        val listState = rememberLazyListState()
                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val layout = listState.layoutInfo
                                val last = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
                                last >= layout.totalItemsCount - 3
                            }
                        }
                        LaunchedEffect(shouldLoadMore) {
                            if (shouldLoadMore) viewModel.loadMore()
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("records_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            item(key = "stats") {
                                RecordsStatsCard(
                                    total = state.records.size,
                                    correct = state.correctCount,
                                    incorrect = state.incorrectCount,
                                    accuracy = state.accuracy,
                                )
                            }
                            item(key = "filters") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AnswerFilter.entries.forEach { filter ->
                                        FilterChip(
                                            selected = state.filter == filter,
                                            onClick = { viewModel.setFilter(filter) },
                                            label = { Text(filter.label) },
                                            modifier = Modifier.testTag("record_filter_${filter.name}"),
                                        )
                                    }
                                }
                            }
                            items(state.records, key = { it.id }) { record ->
                                RecordCard(
                                    record = record,
                                    onClick = { viewModel.openRecord(record) },
                                )
                            }
                            item(key = "load_more_footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else if (!state.hasMore) {
                                        Text(
                                            text = "没有更多了",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
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

    state.selected?.let { record ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSheet,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            RecordDetailSheet(
                record = record,
                question = state.selectedQuestion,
                loading = state.detailLoading,
            )
        }
    }
}

@Composable
private fun RecordsStatsCard(total: Int, correct: Int, incorrect: Int, accuracy: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    "正确率",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$accuracy%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            StatDivider()
            StatBlock(label = "当前记录", value = total.toString(), modifier = Modifier.weight(1f))
            StatDivider()
            StatBlock(label = "答对", value = correct.toString(), modifier = Modifier.weight(1f))
            StatDivider()
            StatBlock(label = "答错", value = incorrect.toString(), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun RecordCard(record: AnswerRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_${record.id}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(type = record.question.type)
                Spacer(Modifier.weight(1f))
                Text(
                    text = record.createdAt.toRecordLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = record.question.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "我的答案：${record.answer.joinToString("、").ifEmpty { "未作答" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                ResultBadge(correct = record.correct)
            }
        }
    }
}

@Composable
private fun RecordDetailSheet(
    record: AnswerRecord,
    question: com.biglv.recitelsland.data.model.Question?,
    loading: Boolean,
) {
    val detail = question ?: record.question
    if (loading && question == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(type = record.question.type)
            Spacer(Modifier.width(8.dp))
            ResultBadge(correct = record.correct)
            Spacer(Modifier.weight(1f))
            Text(
                text = record.createdAt.toRecordLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = detail.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))

        SectionLabel("题目")
        MarkdownText(content = detail.context)

        if (detail.type != QuestionType.Reading && detail.options.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionLabel("选项")
            detail.options.forEachIndexed { index, option ->
                val letter = ('A' + index).toString()
                ReadOnlyOptionRow(
                    letter = letter,
                    text = option,
                    selected = letter in record.answer,
                    correct = letter in detail.answer,
                )
                if (index != detail.options.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("作答结果")
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = "我的答案：${record.answer.joinToString("、").ifEmpty { "未作答" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (detail.answer.isNotEmpty()) {
                    Text(
                        text = "正确答案：${detail.answer.joinToString("、")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (detail.explanation.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            SectionLabel("解析")
            MarkdownText(content = detail.explanation)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun ReadOnlyOptionRow(
    letter: String,
    text: String,
    selected: Boolean,
    correct: Boolean,
) {
    val semantic = LocalSemanticColors.current
    val container: Color = when {
        correct -> semantic.correctContainer
        selected -> semantic.incorrectContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val content: Color = when {
        correct -> semantic.onCorrectContainer
        selected -> semantic.onIncorrectContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border: Color = when {
        correct -> semantic.correct
        selected -> semantic.incorrect
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border = BorderStroke(if (correct || selected) 1.5.dp else 1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (correct || selected) border else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (correct || selected) {
                    MaterialTheme.colorScheme.surface
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
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (correct) {
                Text(
                    text = "正确",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.correct,
                )
            } else if (selected) {
                Text(
                    text = "你的选择",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.incorrect,
                )
            }
        }
    }
}
