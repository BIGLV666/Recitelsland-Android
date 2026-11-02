package com.biglv.recitelsland.feature.bank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biglv.recitelsland.ReciteApplication
import com.biglv.recitelsland.core.ui.simpleViewModelFactory
import com.biglv.recitelsland.data.model.QuestionFilter
import com.biglv.recitelsland.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankScreen(
    onQuestionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: BankViewModel = viewModel(
        factory = simpleViewModelFactory { BankViewModel(app.container.repository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val subtitle = when {
        state.query.isNotBlank() -> "搜索“${state.query.trim()}”"
        state.selectedTag != null -> "标签 #${state.selectedTag}"
        else -> null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "题库",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleSearch,
                        modifier = Modifier.testTag("action_search"),
                    ) {
                        Icon(
                            imageVector = if (state.searchVisible) Icons.Filled.Close else Icons.Outlined.Search,
                            contentDescription = if (state.searchVisible) "关闭搜索" else "搜索",
                        )
                    }
                    IconButton(
                        onClick = viewModel::toggleTags,
                        modifier = Modifier.testTag("action_tags"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Label,
                            contentDescription = "标签筛选",
                            tint = if (state.tagsExpanded || state.selectedTag != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            AnimatedVisibility(
                visible = state.searchVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_field"),
                    placeholder = { Text("搜索标题、题干或标签") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "清空搜索")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
            }

            AnimatedVisibility(
                visible = state.tagsExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                TagFilterPanel(
                    state = state,
                    onSelectTag = viewModel::setTag,
                    onLoadMore = viewModel::loadMoreTags,
                    onCollapse = viewModel::toggleTags,
                )
            }

            if (!state.loading) {
                TypeFilterRow(state = state, onSelect = viewModel::setFilter)
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    state.visibleQuestions.isEmpty() -> {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            title = if (state.questions.isEmpty()) "题库还没有题目" else "没有匹配的题目",
                            modifier = Modifier.align(Alignment.Center),
                        )
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
                                .testTag("bank_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.visibleQuestions, key = { it.id }) { question ->
                                QuestionSummaryCard(
                                    question = question,
                                    onClick = { onQuestionClick(question.id) },
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
                                    } else if (!state.hasMore && state.visibleQuestions.isNotEmpty()) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeFilterRow(
    state: BankUiState,
    onSelect: (QuestionFilter) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuestionFilter.entries.forEach { filter ->
            FilterChip(
                selected = state.filter == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
                modifier = Modifier.testTag("filter_${filter.name}"),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFilterPanel(
    state: BankUiState,
    onSelectTag: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onCollapse: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("tag_panel"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCollapse) {
                    Text("收起")
                }
            }

            if (state.tags.isEmpty() && state.loadingTags) {
                Text(
                    text = "标签加载中",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = state.selectedTag == null,
                        onClick = { onSelectTag(null) },
                        label = { Text("全部") },
                        modifier = Modifier.testTag("tag_filter_all"),
                    )
                    state.tags.forEach { tag ->
                        FilterChip(
                            selected = state.selectedTag == tag.name,
                            onClick = { onSelectTag(tag.name) },
                            label = {
                                Text(
                                    if (tag.questionCount > 0) {
                                        "#${tag.name} ${tag.questionCount}"
                                    } else {
                                        "#${tag.name}"
                                    },
                                )
                            },
                            modifier = Modifier.testTag("tag_filter_${tag.name}"),
                        )
                    }
                    if (state.hasMoreTags) {
                        FilterChip(
                            selected = false,
                            onClick = onLoadMore,
                            label = { Text("更多") },
                            modifier = Modifier.testTag("tag_filter_more"),
                        )
                    }
                }
            }
        }
    }
}
