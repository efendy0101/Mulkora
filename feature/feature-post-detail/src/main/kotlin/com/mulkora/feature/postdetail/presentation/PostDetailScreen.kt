package com.mulkora.feature.postdetail.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.feature.postdetail.domain.model.PostDetail
import com.mulkora.feature.reactions.RatingDialog

/**
 * Gönderi detay ekranı.
 *
 * SCR-054: Gönderi detayı — başlık, gövde, yazar, etkileşim sayaçları.
 * CLAUDE.md §9: Beğeni, eksi oy ve yıldız puanlama bu ekrandan yapılabilir.
 * CLAUDE.md §33: Ekran doğrudan Firestore'a erişemez; ViewModel üzerinden alır.
 *
 * Bu ekran :feature:feature-post-detail modülüne aittir (docs/10-android-module-structure.md).
 * NavGraphs.kt bu modülü import eder; feature-feed'deki eski PostDetailScreen deprecated'dır.
 */
@Composable
fun PostDetailScreen(
    onNavigateUp: () -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToReport: (String) -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveResult by viewModel.saveResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Puanlama dialogu göster/gizle
    var showRatingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveResult) {
        saveResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val successState = uiState as? PostDetailUiState.Success
            PostDetailTopBar(
                title = successState?.post?.title ?: "",
                isSaved = successState?.post?.isSaved ?: false,
                onBack = onNavigateUp,
                onSave = viewModel::toggleSavePost,
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is PostDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is PostDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is PostDetailUiState.Success -> {
                PostDetailContent(
                    post = state.post,
                    modifier = Modifier.padding(innerPadding),
                    onCommentsClick = { onNavigateToComments(state.post.id) },
                    onReportClick = { onNavigateToReport(state.post.id) },
                    onLikeClick = { viewModel.toggleVote(1) },
                    onDislikeClick = { viewModel.toggleVote(-1) },
                    onRateClick = { showRatingDialog = true },
                )

                if (showRatingDialog) {
                    RatingDialog(
                        postTitle = state.post.title,
                        initialRating = state.post.currentUserRating,
                        onConfirm = { rating ->
                            viewModel.ratePost(rating)
                            showRatingDialog = false
                        },
                        onDismiss = { showRatingDialog = false },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailTopBar(
    title: String,
    isSaved: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                )
            }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isSaved) "Kayıttan kaldır" else "Kaydet",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun PostDetailContent(
    post: PostDetail,
    onCommentsClick: () -> Unit,
    onReportClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Başlık
        Text(
            text = post.title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Yazar
        Text(
            text = if (post.isAnonymous) "Anonim" else post.authorDisplayName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // İçerik
        Text(
            text = post.body,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Etkileşim satırı — CLAUDE.md §9: beğeni, eksi oy, yıldız, yorum, şikâyet
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Beğen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(onClick = onLikeClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (post.currentUserVote == 1)
                            Icons.Default.ThumbUp
                        else
                            Icons.Default.ThumbUpOffAlt,
                        contentDescription = "Beğen",
                        tint = if (post.currentUserVote == 1)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = post.likeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            // Beğenme (eksi oy)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(onClick = onDislikeClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (post.currentUserVote == -1)
                            Icons.Default.ThumbDown
                        else
                            Icons.Default.ThumbDownOffAlt,
                        contentDescription = "Beğenme",
                        tint = if (post.currentUserVote == -1)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = post.dislikeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            // Puan ver
            IconButton(onClick = onRateClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Puanla",
                    tint = if (post.currentUserRating != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Yorumlar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(onClick = onCommentsClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Yorumlar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = post.commentCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            // Şikâyet
            IconButton(onClick = onReportClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Şikâyet et",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Ortalama puan gösterimi
        if (post.ratingCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "%.1f".format(post.averageRating) + " (${post.ratingCount} oy)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}