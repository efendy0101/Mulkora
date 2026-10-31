package com.mulkora.feature.reference.presentation.write

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mulkora.feature.reference.domain.model.RelationshipType

/**
 * Referans yazma ekranı.
 * CLAUDE.md §13: Referans state machine — awaiting_confirmation → preparing → pending_approval
 * submitReference Cloud Function tarafından tetiklenir; istemci doğrudan yazmaz.
 * Moderasyon CF tarafında yapılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceWriteScreen(
    onNavigateUp: () -> Unit,
    onNavigateToList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReferenceWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(events) {
        when (events) {
            is ReferenceWriteEvent.NavigateToList -> {
                viewModel.consumeEvent()
                onNavigateToList()
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
                title = { Text("Referans Yaz") },
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
                text = "${uiState.toDisplayName} (@${uiState.toUsername}) için referans yazıyorsunuz.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // İlişki türü seçimi
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value = uiState.relationshipType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("İlişki Türü") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                ) {
                    RelationshipType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                viewModel.onRelationshipTypeChanged(type)
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }

            // Referans metni
            OutlinedTextField(
                value = uiState.commentText,
                onValueChange = viewModel::onCommentChanged,
                label = { Text("Referans Metni") },
                placeholder = { Text("Bu kişiyle olan deneyiminizi paylaşın...") },
                minLines = 5,
                maxLines = 12,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text("${uiState.commentText.length}/1000")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Referans gönderildikten sonra moderasyon sürecinden geçecektir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = viewModel::submitReference,
                enabled = !uiState.isLoading && uiState.commentText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Referansı Gönder")
            }
        }
    }
}

