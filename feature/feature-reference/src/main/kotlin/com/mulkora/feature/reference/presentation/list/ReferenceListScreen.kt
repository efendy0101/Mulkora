package com.mulkora.feature.reference.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.core.common.model.ReferenceStatus
import com.mulkora.feature.reference.domain.model.Reference

/**
 * Referans Listesi ekranı.
 * CLAUDE.md Bölüm 5: Referans sistemi — alınan, verilen ve bekleyen referanslar.
 * Referans durumu state machine'e göre gösterilir (docs/16-reference-system-state-machine.md).
 *
 * KURAL: PENDING sekmesinde tıklama → ReferenceRecognitionScreen (tanıma onayı adımı).
 *        Diğer sekmelerde tıklama   → ReferenceDetailScreen (detay ekranı).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceListScreen(
    onNavigateUp: () -> Unit,
    onNavigateToReferenceDetail: (String) -> Unit,
    onNavigateToRecognition: (String) -> Unit = {},
    viewModel: ReferenceListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referanslarım") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Sekme satırı
            val tabs = listOf("Alınan", "Verilen", "Bekleyen")
            val selectedIndex = uiState.selectedTab.ordinal

            TabRow(selectedTabIndex = selectedIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { viewModel.onTabSelected(ReferenceTab.entries[index]) },
                        text = { Text(title) },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.references.isEmpty() -> {
                        Text(
                            text = "Bu sekmede referans bulunamadı.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = uiState.references,
                                key = { it.id },
                            ) { reference ->
                                ReferenceCard(
                                    reference = reference,
                                    tab = uiState.selectedTab,
                                    onClick = {
                                        // PENDING sekmesindeki referanslar tanıma onayına gider
                                        if (uiState.selectedTab == ReferenceTab.PENDING) {
                                            onNavigateToRecognition(reference.id)
                                        } else {
                                            onNavigateToReferenceDetail(reference.id)
                                        }
                                    },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceCard(
    reference: Reference,
    tab: ReferenceTab,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Karşı taraf bilgisi
            Row(verticalAlignment = Alignment.CenterVertically) {
                val otherName = when (tab) {
                    ReferenceTab.RECEIVED -> reference.fromDisplayName
                    ReferenceTab.GIVEN -> reference.toDisplayName
                    ReferenceTab.PENDING -> reference.fromDisplayName
                }
                val otherUsername = when (tab) {
                    ReferenceTab.RECEIVED -> reference.fromUsername
                    ReferenceTab.GIVEN -> reference.toUsername
                    ReferenceTab.PENDING -> reference.fromUsername
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = otherName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "@$otherUsername",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Durum etiketi
                ReferenceStatusChip(status = reference.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // İlişki türü
            Text(
                text = reference.relationshipType.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Puan (yıldızlar)
            if (tab != ReferenceTab.PENDING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(reference.rating) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    repeat(5 - reference.rating) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${reference.rating}/5",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Referans türü etiketi
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = reference.referenceType.displayName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ReferenceStatusChip(status: ReferenceStatus) {
    val (label, containerColor) = when (status) {
        ReferenceStatus.INVITE_SENT           -> "Davet Gönderildi" to MaterialTheme.colorScheme.tertiaryContainer
        ReferenceStatus.AWAITING_CONFIRMATION -> "Onay Bekleniyor" to MaterialTheme.colorScheme.tertiaryContainer
        ReferenceStatus.NOT_RECOGNIZED        -> "Tanınmadı" to MaterialTheme.colorScheme.errorContainer
        ReferenceStatus.PREPARING             -> "Yazılıyor" to MaterialTheme.colorScheme.secondaryContainer
        ReferenceStatus.PENDING_APPROVAL      -> "İncelemede" to MaterialTheme.colorScheme.secondaryContainer
        ReferenceStatus.APPROVED              -> "Onaylı" to MaterialTheme.colorScheme.primaryContainer
        ReferenceStatus.REJECTED              -> "Reddedildi" to MaterialTheme.colorScheme.errorContainer
        ReferenceStatus.APPEALED              -> "İtiraz Edildi" to MaterialTheme.colorScheme.tertiaryContainer
        ReferenceStatus.UNDER_REVIEW          -> "İtiraz İncelemede" to MaterialTheme.colorScheme.secondaryContainer
        ReferenceStatus.HIDDEN                -> "Gizlendi" to MaterialTheme.colorScheme.surfaceVariant
        ReferenceStatus.DELETED               -> "Silindi" to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
