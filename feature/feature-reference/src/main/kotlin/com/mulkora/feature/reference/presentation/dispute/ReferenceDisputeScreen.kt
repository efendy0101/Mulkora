package com.mulkora.feature.reference.presentation.dispute

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Referans itiraz ekranı.
 * CLAUDE.md §13: approved → disputed durum geçişi.
 * disputeReference Cloud Function üzerinden yapılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceDisputeScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReferenceDisputeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(events) {
        when (events) {
            is ReferenceDisputeEvent.NavigateBack -> {
                viewModel.consumeEvent()
                onNavigateUp()
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
                title = { Text("Referansa İtiraz Et") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Bu referans size haksız yere verildiğini düşünüyorsanız gerekçenizi belirterek itiraz edebilirsiniz.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = uiState.disputeReason,
                onValueChange = viewModel::onReasonChanged,
                label = { Text("İtiraz Gerekçesi") },
                placeholder = { Text("Bu referansın neden haksız olduğunu açıklayın...") },
                minLines = 4,
                maxLines = 10,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text("${uiState.disputeReason.length}/500")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "İtiraz moderatörlerimiz tarafından incelenecek ve size geri dönüş yapılacaktır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = viewModel::submitDispute,
                enabled = !uiState.isLoading && uiState.disputeReason.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("İtirazı Gönder")
            }
        }
    }
}