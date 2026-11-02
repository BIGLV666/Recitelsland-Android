package com.biglv.recitelsland.feature.favorites

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
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
import com.biglv.recitelsland.core.ui.toRecordLabel
import com.biglv.recitelsland.data.model.FavoriteItem
import com.biglv.recitelsland.ui.components.EmptyState
import com.biglv.recitelsland.ui.components.TagText
import com.biglv.recitelsland.ui.components.TypeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ReciteApplication
    val viewModel: FavoritesViewModel = viewModel(
        factory = simpleViewModelFactory { FavoritesViewModel(app.container.repository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("我的收藏", style = MaterialTheme.typography.titleLarge) },
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
                                    icon = Icons.Outlined.BookmarkBorder,
                                    title = "还没有收藏",
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("favorites_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.items, key = { it.favoriteId }) { item ->
                                FavoriteCard(
                                    item = item,
                                    onOpen = { onOpenQuestion(item.question.id) },
                                    onRemove = { viewModel.remove(item.favoriteId) },
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
}

@Composable
private fun FavoriteCard(
    item: FavoriteItem,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("favorite_${item.favoriteId}")
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(type = item.question.type)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("remove_favorite_${item.favoriteId}"),
                ) {
                    Text("取消收藏")
                }
            }
            Text(
                text = item.question.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.question.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp),
            )
            if (item.question.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.question.tags.take(3).forEach { TagText(tag = it) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "收藏于 ${item.createdAt.toRecordLabel()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
