package com.mulkora.feature.feed.presentation.postedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Gönderi düzenleme ekranı (SCR-055).
 *
 * docs/17-post-moderation-state-machine.md:
 *   edit_requested → pending_approval geçişi yapılır.
 * CLAUDE.md §6: Gönderi yalnızca edit_requested durumundayken düzenlenebilir.
 *
 * updatePost Cloud Function çağrısı ViewModel üzerinden yapılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostEditScreen(
    onNavigateUp: () -> Unit,
    onEditSubmitted: () -> Unit,
    viewModel: PostEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Başarılı gönderi sonrası geri git
    LaunchedEffect(uiState.submitted) {
        if (uiState.submitted) {
            onEditSubmitted()
        }
    }

    // Hata mesajı göster
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gönderi Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading) {
                        IconButton(
                            onClick = viewModel::onSubmitEdit,
                            enabled = uiState.isFormValid,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Gönder",
                                tint = if (uiState.isFormValid)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Başlık alanı
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Başlık") },
                placeholder = { Text("3–150 karakter") },
                supportingText = {
                    Text(
                        text = "${uiState.title.length}/150",
                        color = if (uiState.title.length > 150)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                isError = uiState.title.length > 150,
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // İçerik alanı
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChange,
                label = { Text("İçerik") },
                placeholder = { Text("Düzenleme notunuzu buraya yazın...") },
                supportingText = {
                    Text(
                        text = "${uiState.content.length}/5000",
                        color = if (uiState.content.length > 5000)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                isError = uiState.content.length > 5000,
                minLines = 6,
                maxLines = 20,
                modifier = Modifier.fillMaxWidth(),
            )

            // Moderatör notu (salt okunur, eğer varsa)
            if (uiState.moderatorNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Moderatör Notu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.moderatorNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::onSubmitEdit,
                enabled = uiState.isFormValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Düzenlemeyi Gönder")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}