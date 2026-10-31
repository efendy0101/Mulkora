package com.mulkora.feature.explore.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.core.common.model.PostCategory

/**
 * Kategoriye göre filtrelenmiş gönderi listesi ekranı.
 *
 * SCR-031: Keşfet — Kategori gönderileri.
 * CLAUDE.md §7: Onaylı (approved) gönderiler listelenir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPostsScreen(
    category: PostCategory,
    onNavigateUp: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    viewModel: CategoryPostsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.displayName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is CategoryPostsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is CategoryPostsUiState.Error -> {
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

            is CategoryPostsUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Bu kategoride henüz gönderi yok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is CategoryPostsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    items(state.posts, key = { it.id }) { post ->
                        ExplorePostItem(
                            title = post.title,
                            authorName = post.authorDisplayName,
                            likeCount = post.likeCount,
                            commentCount = post.commentCount,
                            onClick = { onNavigateToPostDetail(post.id) },
                        )
                    }
                }
            }
        }
    }
}