package com.mulkora.feature.reference.presentation.recognition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Referans tanıma adımı ekranı.
 * CLAUDE.md §13: invite_sent → awaiting_confirmation veya not_recognized
 * Kullanıcı referans verenin kim olduğunu görür ve onaylar ya da reddeder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceRecognitionScreen(
    onNavigateUp: () -> Unit,
    onNavigateToWrite: (referenceId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReferenceRecognitionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(events) {
        when (events) {
            is ReferenceRecognitionEvent.NavigateBack -> {
                viewModel.consumeEvent()
                onNavigateUp()
            }
            is ReferenceRecognitionEvent.NavigateToResponse -> {
                viewModel.consumeEvent()
                onNavigateToWrite(uiState.referenceId)
            }
            null -> Unit
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referansı Onayla") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text(
                        text = "Referans Talebi",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = "${uiState.fromDisplayName} (@${uiState.fromUsername}) size bir referans vermek istiyor.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = "Bu kişiyi tanıyor musunuz? Onayladığınızda referansı yazmaları için bir adım ilerleyecekler.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OutlinedButton(
                            onClick = viewModel::rejectRecognition,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Tanımıyorum")
                        }

                        Button(
                            onClick = viewModel::acceptReference,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Tanıyorum")
                        }
                    }
                }
            }
        }
    }
}