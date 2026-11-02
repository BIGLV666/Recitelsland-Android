package com.biglv.recitelsland.feature.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.data.model.ImportQuestionResult
import com.biglv.recitelsland.data.model.Question
import com.biglv.recitelsland.data.model.QuestionType
import com.biglv.recitelsland.ui.components.TagText
import com.biglv.recitelsland.ui.components.TypeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuestionsScreen(
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onImport: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: AdminQuestionsViewModel = viewModel(
        factory = simpleViewModelFactory {
            AdminQuestionsViewModel(app.container.repository, app.container.adminRepository)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Question?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load(reset = true)
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("题库管理", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    TextButton(onClick = onNew) { Text("新建") }
                    TextButton(onClick = onImport) { Text("导入") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )

            if (state.loading) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.id }) { question ->
                        AdminQuestionCard(
                            question = question,
                            onEdit = { onEdit(question.id) },
                            onDelete = { pendingDelete = question },
                        )
                    }
                    if (state.hasMore) {
                        item(key = "more") {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                if (state.loadingMore) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                else TextButton(onClick = viewModel::loadMore) { Text("加载更多") }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    pendingDelete?.let { question ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除题目") },
            text = { Text("确定删除“${question.title}”吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.delete(question.id)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun AdminQuestionCard(
    question: Question,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(question.type)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                question.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                question.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (question.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.tags.take(4).forEach { TagText(it) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditorScreen(
    questionId: Long?,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: QuestionEditorViewModel = viewModel(
        factory = simpleViewModelFactory {
            QuestionEditorViewModel(app.container.repository, app.container.adminRepository, questionId)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Text(if (questionId == null) "新建题目" else "编辑题目", style = MaterialTheme.typography.titleLarge)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )

            if (state.loading) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuestionType.entries.forEach { type ->
                            FilterChip(
                                selected = state.type == type,
                                onClick = { viewModel.setType(type) },
                                enabled = !state.editing,
                                label = { Text(type.label) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        modifier = Modifier.fillMaxWidth().testTag("question_title"),
                        label = { Text("标题") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.context,
                        onValueChange = viewModel::setContext,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("题干 / 阅读材料") },
                        minLines = 5,
                    )
                    if (state.type != QuestionType.Reading) {
                        OutlinedTextField(
                            value = state.optionsText,
                            onValueChange = viewModel::setOptions,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("选项，一行一个") },
                            minLines = 4,
                        )
                        OutlinedTextField(
                            value = state.answerText,
                            onValueChange = viewModel::setAnswer,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("正确答案，如 A 或 A,B") },
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = state.explanation,
                        onValueChange = viewModel::setExplanation,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("解析") },
                        minLines = 4,
                    )
                    OutlinedTextField(
                        value = state.tagsText,
                        onValueChange = viewModel::setTags,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("标签，逗号分隔") },
                        singleLine = true,
                    )
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("保存")
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportQuestionsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: ImportQuestionsViewModel = viewModel(
        factory = simpleViewModelFactory { ImportQuestionsViewModel(app.container.adminRepository) },
    )
    val text by viewModel.text.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("批量导入", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = viewModel::setText,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("题目 JSON 数组或 {\"questions\": [...]}") },
                    minLines = 12,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    placeholder = {
                        Text(
                            """[{"questionTitle":"标题","questionContext":"题干","questionType":1,"answer":["A"],"options":["选项A","选项B"],"explanation":"解析","tags":["标签"]}]""",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                )
                Button(
                    onClick = viewModel::submit,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("导入")
                    }
                }
                result?.let { ImportResultCard(it) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun ImportResultCard(result: ImportQuestionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("总数 ${result.total}", style = MaterialTheme.typography.bodyMedium)
            Text("导入 ${result.imported}", style = MaterialTheme.typography.bodyMedium)
            Text("跳过 ${result.skipped}", style = MaterialTheme.typography.bodyMedium)
            Text("失败 ${result.failed}", style = MaterialTheme.typography.bodyMedium)
            result.errors.take(10).forEach {
                Text("第 ${it.index} 条：${it.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
