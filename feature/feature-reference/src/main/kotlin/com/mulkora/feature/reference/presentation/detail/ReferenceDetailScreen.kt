package com.mulkora.feature.reference.presentation.detail

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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.feature.reference.domain.model.Reference
import com.mulkora.feature.reference.domain.model.ReferenceResponse

/**
 * Referans detay ekranı.
 *
 * CLAUDE.md §13:
 * - Onaylı referansı, referans cevabını ve temel bilgileri gösterir.
 * - Referans alıcısı (toUid) henüz cevap vermemişse "Cevap Ver" butonu görünür.
 * - Referans alıcısı (toUid) "İtiraz Et" butonuna basarak itiraz başlatabilir.
 * - Bu aksiyonlar yalnızca alıcıya gösterilir; Firestore Security Rules + Cloud Functions
 *   ile de güvence altına alınmıştır.
 *
 * docs/16-reference-system-state-machine.md: approved durumunda aksiyon butonları aktif.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceDetailScreen(
    onNavigateUp: () -> Unit,
    onNavigateToResponse: (referenceId: String) -> Unit,
    onNavigateToAppeal: (referenceId: String) -> Unit,
    viewModel: ReferenceDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referans Detayı") },
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "Hata oluştu.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                    )
                }
                uiState.reference != null -> {
                    ReferenceDetailContent(
                        reference = uiState.reference!!,
                        canRespond = uiState.canRespond,
                        canAppeal = uiState.canAppeal,
                        onRespondClick = {
                            onNavigateToResponse(uiState.reference!!.id)
                        },
                        onAppealClick = {
                            onNavigateToAppeal(uiState.reference!!.id)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceDetailContent(
    reference: Reference,
    canRespond: Boolean,
    canAppeal: Boolean,
    onRespondClick: () -> Unit,
    onAppealClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Başlık — kim kimden referans vermiş
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = reference.referenceType.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${reference.fromDisplayName} (@${reference.fromUsername})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "→ ${reference.toDisplayName} (@${reference.toUsername})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Puan
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Puan: ",
                style = MaterialTheme.typography.labelLarge,
            )
            (1..5).forEach { star ->
                Icon(
                    imageVector = if (star <= reference.rating) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = null,
                    tint = if (star <= reference.rating) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = "(${reference.rating}/5)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // İlişki türü
        LabeledInfo(label = "İlişki Türü", value = reference.relationshipType.displayName)

        // Mülk adresi (varsa)
        reference.propertyAddress?.let { address ->
            if (address.isNotBlank()) {
                LabeledInfo(label = "Mülk Adresi", value = address)
            }
        }

        HorizontalDivider()

        // Referans metni
        Column {
            Text(
                text = "Referans",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reference.content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Referans cevabı (varsa)
        reference.response?.let { response ->
            ReferenceResponseCard(response = response)
        }

        // Aksiyon butonları — yalnızca referans alıcısına gösterilir
        if (canRespond || canAppeal) {
            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Cevap Ver butonu — yalnızca cevap henüz yazılmamışsa
                if (canRespond) {
                    Button(
                        onClick = onRespondClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Referansa Cevap Ver")
                    }
                }

                // İtiraz Et butonu
                if (canAppeal) {
                    OutlinedButton(
                        onClick = onAppealClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Referansa İtiraz Et")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ReferenceResponseCard(response: ReferenceResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Yanıt",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = response.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun LabeledInfo(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}