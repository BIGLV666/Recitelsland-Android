package com.biglv.recitelsland.feature.question

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.core.ui.toRecordLabel
import com.biglv.recitelsland.data.model.AnswerRecord
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.ui.components.EmptyState
import com.biglv.recitelsland.ui.components.ResultBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    questionId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: QuestionDetailViewModel = viewModel(
        factory = simpleViewModelFactory {
            QuestionDetailViewModel(app.container.repository, questionId)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val question = state.question
    val recordsTitle = if (state.recordsLoaded) "记录 ${state.records.size}" else "记录"

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "题目详情",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = question?.type?.label ?: "正在加载",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            if (question != null) {
                TabRow(
                    selectedTabIndex = state.tab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Tab(
                        selected = state.tab == DetailTab.Question,
                        onClick = { viewModel.selectTab(DetailTab.Question) },
                        text = { Text(DetailTab.Question.label) },
                        modifier = Modifier.testTag("detail_tab_question"),
                    )
                    Tab(
                        selected = state.tab == DetailTab.Records,
                        onClick = { viewModel.selectTab(DetailTab.Records) },
                        text = { Text(recordsTitle) },
                        modifier = Modifier.testTag("detail_tab_records"),
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    question == null -> {
                        Text(
                            text = "题目不存在或已失效",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    state.tab == DetailTab.Question -> {
                        QuestionDetailTab(
                            question = question,
                            card = state.card,
                            favorite = state.favoriteId != null,
                            inReview = state.inReview,
                            onToggleOption = viewModel::toggleOption,
                            onSubmit = viewModel::submit,
                            onCompleteReading = viewModel::completeReading,
                            onRetryAnswer = viewModel::retryAnswer,
                            onFavorite = viewModel::toggleFavorite,
                            onAddReview = viewModel::addReview,
                        )
                    }

                    else -> {
                        QuestionRecordsTab(
                            question = question,
                            records = state.records,
                            loading = state.recordsLoading,
                        )
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
private fun QuestionDetailTab(
    question: Question,
    card: AnswerCardState,
    favorite: Boolean,
    inReview: Boolean,
    onToggleOption: (String) -> Unit,
    onSubmit: () -> Unit,
    onCompleteReading: () -> Unit,
    onRetryAnswer: () -> Unit,
    onFavorite: () -> Unit,
    onAddReview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 32.dp),
    ) {
        QuestionAnswerContent(
            question = question,
            card = card,
            favorite = favorite,
            inReview = inReview,
            onToggleOption = onToggleOption,
            onSubmit = onSubmit,
            onCompleteReading = onCompleteReading,
            onRetryAnswer = onRetryAnswer,
            onFavorite = onFavorite,
            onAddReview = onAddReview,
        )
    }
}

@Composable
private fun QuestionRecordsTab(
    question: Question,
    records: List<AnswerRecord>,
    loading: Boolean,
) {
    when {
        loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        records.isEmpty() -> {
            EmptyState(
                icon = Icons.Outlined.History,
                title = "还没有作答记录",
                description = if (question.type == QuestionType.Reading) {
                    "阅读题不产生作答记录，完成学习只累加学习次数。"
                } else {
                    "回到“题目”页作答后，这里会显示这道题的历史记录。"
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("question_records_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") {
                    RecordsSummary(records = records)
                }
                itemsIndexed(records, key = { _, record -> record.id }) { index, record ->
                    QuestionRecordCard(
                        record = record,
                        attemptNumber = records.size - index,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordsSummary(records: List<AnswerRecord>) {
    val correct = records.count { it.correct }
    val incorrect = records.size - correct
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "这道题的作答记录",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "共 ${records.size} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "答对 $correct · 答错 $incorrect",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuestionRecordCard(
    record: AnswerRecord,
    attemptNumber: Int,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("question_record_${record.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "第 $attemptNumber 次作答",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                ResultBadge(correct = record.correct)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "我的答案：${record.answer.joinToString("、").ifEmpty { "未作答" }}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "正确答案：${record.question.answer.joinToString("、").ifEmpty { "见解析" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = record.createdAt.toRecordLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
