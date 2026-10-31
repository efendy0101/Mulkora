package com.mulkora.feature.feed.presentation.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.mulkora.core.common.model.PostType
import com.mulkora.feature.feed.domain.model.FeedItem
import com.mulkora.feature.feed.domain.model.ReactionType
import com.mulkora.feature.feed.domain.repository.FeedSortOption

/**
 * Ana feed ekranı.
 * CLAUDE.md §6: Onaylı gönderileri listeler, reklam ara sıra eklenir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToReport: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedItems: LazyPagingItems<FeedItem> = viewModel.feedItems.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mülkora",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreatePost,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Gönderi Oluştur"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        val filterState by viewModel.filterState.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Filtre çubuğu: Sıralama ---
            FeedFilterBar(
                selectedSort = filterState.sortOption,
                selectedPostType = filterState.selectedPostType,
                onSortSelected = viewModel::onSortOptionSelected,
                onPostTypeSelected = viewModel::onPostTypeSelected
            )

            HorizontalDivider()

            Box(modifier = Modifier.weight(1f)) {
                // İçerik
                FeedContent(
                    feedItems = feedItems,
                    onPostClick = onNavigateToPostDetail,
                    onAuthorClick = onNavigateToUserProfile,
                    onReactionClick = viewModel::onReactionClick,
                    onSaveClick = viewModel::onSavePostClick,
                    onReportClick = onNavigateToReport,
                    onShareClick = { postId ->
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "Bu gönderiyi Mülkora'da gör: https://mulkora.app/posts/$postId"
                            )
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(shareIntent, "Paylaş")
                        )
                    }
                )

                // Yükleniyor göstergesi (ilk yükleme)
                if (feedItems.loadState.refresh is LoadState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Hata durumu (ilk yükleme)
                if (feedItems.loadState.refresh is LoadState.Error) {
                    val error = feedItems.loadState.refresh as LoadState.Error
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Gönderiler yüklenemedi",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = error.error.message ?: "Bilinmeyen hata",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Yatay filtre çubuğu.
 * CLAUDE.md §6: Sıralama ve gönderi türü filtreleri.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedFilterBar(
    selectedSort: FeedSortOption,
    selectedPostType: PostType?,
    onSortSelected: (FeedSortOption) -> Unit,
    onPostTypeSelected: (PostType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Sıralama chip'leri
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FeedSortOption.entries) { sort ->
                FilterChip(
                    selected = sort == selectedSort,
                    onClick = { onSortSelected(sort) },
                    label = {
                        Text(
                            text = sort.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        // Gönderi türü chip'leri — "Tümü" + her PostType
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedPostType == null,
                    onClick = { onPostTypeSelected(null) },
                    label = {
                        Text(
                            text = "Tümü",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
            items(PostType.entries) { postType ->
                FilterChip(
                    selected = postType == selectedPostType,
                    onClick = { onPostTypeSelected(postType) },
                    label = {
                        Text(
                            text = postType.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FeedContent(
    feedItems: LazyPagingItems<FeedItem>,
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onReactionClick: (String, ReactionType) -> Unit,
    onSaveClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = feedItems.itemCount,
            key = { index ->
                val item = feedItems.peek(index)
                when (item) {
                    is FeedItem.PostItem -> "post_${item.post.id}"
                    is FeedItem.NativeAdItem -> "ad_$index"
                    null -> "loading_$index"
                }
            }
        ) { index ->
            when (val item = feedItems[index]) {
                is FeedItem.PostItem -> {
                    PostCard(
                        post = item.post,
                        onPostClick = { onPostClick(item.post.id) },
                        onAuthorClick = { onAuthorClick(item.post.authorUid) },
                        onReactionClick = { reactionType ->
                            onReactionClick(item.post.id, reactionType)
                        },
                        onSaveClick = { onSaveClick(item.post.id) },
                        onReportClick = { onReportClick(item.post.id) },
                        onShareClick = { onShareClick(item.post.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                is FeedItem.NativeAdItem -> {
                    // Native reklam — AdMob NativeAd bileşeni
                    // TODO: NativeAdView Compose wrapper ile tamamlanacak
                    NativeAdPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
                null -> Unit
            }
        }

        // Daha fazla yükleniyor
        if (feedItems.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Native reklam yer tutucu.
 * TODO: Gerçek AdMob NativeAdView ile değiştirilecek.
 */
@Composable
private fun NativeAdPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "[Reklam]",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}