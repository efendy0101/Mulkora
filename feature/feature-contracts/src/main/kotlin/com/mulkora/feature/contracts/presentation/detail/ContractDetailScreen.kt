package com.mulkora.feature.contracts.presentation.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mulkora.feature.contracts.domain.model.Contract
import com.mulkora.feature.contracts.domain.model.ContractStatus
import com.mulkora.feature.contracts.domain.model.PdfStatus

/**
 * Sözleşme detay ekranı.
 *
 * CLAUDE.md §14: Sözleşme akışı — onay, düzenleme talebi, PDF oluşturma.
 * CLAUDE.md §26: Taraf olmayan kişi PDF ve sözleşme içeriğine erişemez
 * (Firestore Rules ile de korunur; UI katmanında ek bir kontrol gerekmez).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ContractDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ContractDetailEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(event.message)

                is ContractDetailEvent.OpenPdfUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                    context.startActivity(intent)
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sözleşme Detayı") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.contract == null -> Text(
                    text = "Sözleşme bulunamadı.",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> {
                    ContractDetailContent(
                        contract = uiState.contract!!,
                        isActionLoading = uiState.isActionLoading,
                        canApprove = uiState.canApprove,
                        canRequestEdit = uiState.canRequestEdit,
                        canGeneratePdf = uiState.canGeneratePdf,
                        onApprove = viewModel::approveContract,
                        onRequestEdit = viewModel::showEditDialog,
                        onGeneratePdf = viewModel::generatePdf,
                    )
                }
            }
        }
    }

    // Düzenleme talebi diyaloğu
    if (uiState.showEditRequestDialog) {
        EditRequestDialog(
            onConfirm = { note ->
                viewModel.requestEdit(note)
                viewModel.hideEditDialog()
            },
            onDismiss = viewModel::hideEditDialog,
        )
    }
}

@Composable
private fun ContractDetailContent(
    contract: Contract,
    isActionLoading: Boolean,
    canApprove: Boolean,
    canRequestEdit: Boolean,
    canGeneratePdf: Boolean,
    onApprove: () -> Unit,
    onRequestEdit: () -> Unit,
    onGeneratePdf: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Durum satırı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = contract.status.toDisplayName(),
                style = MaterialTheme.typography.labelLarge,
                color = contract.status.toColor(),
            )
            Text(
                text = "v${contract.version}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Divider()

        SectionTitle("Taşınmaz Bilgileri")
        InfoRow("Adres", contract.propertyAddress)
        InfoRow("Tür", contract.propertyType)

        Divider()

        SectionTitle("Kira Koşulları")
        InfoRow("Aylık Kira", "₺${contract.monthlyRent}")
        InfoRow("Ödeme Günü", "${contract.paymentDay}. gün")
        InfoRow("Ödeme Yöntemi", contract.paymentMethod)
        InfoRow("Depozito", "₺${contract.depositAmount}")
        contract.monthlyFee?.let { InfoRow("Aidat", "₺$it") }

        Divider()

        SectionTitle("Dönem")
        InfoRow("Başlangıç", contract.startDate)
        InfoRow("Bitiş", contract.endDate)

        Divider()

        SectionTitle("Taraflar")
        InfoRow("Taraf 1", "${contract.party1DisplayName} — ${if (contract.party1Approved) "Onayladı" else "Onay Bekliyor"}")
        contract.party2DisplayName?.let {
            InfoRow("Taraf 2", "$it — ${if (contract.party2Approved) "Onayladı" else "Onay Bekliyor"}")
        }

        if (!contract.inventoryItems.isEmpty()) {
            Divider()
            SectionTitle("Demirbaşlar")
            contract.inventoryItems.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        contract.specialConditions?.let {
            Divider()
            SectionTitle("Özel Şartlar")
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }

        Divider()

        SectionTitle("Belge Bilgileri")
        InfoRow("Belge No", contract.documentNumber)
        InfoRow("Doğrulama Kodu", contract.verificationCode)

        // PDF durumu
        if (contract.status == ContractStatus.LOCKED) {
            Divider()
            SectionTitle("PDF")
            Text(
                text = when (contract.pdfStatus) {
                    PdfStatus.NOT_GENERATED -> "PDF henüz oluşturulmadı."
                    PdfStatus.GENERATING -> "PDF oluşturuluyor..."
                    PdfStatus.READY -> "PDF hazır."
                    PdfStatus.FAILED -> "PDF oluşturulamadı. Lütfen tekrar deneyin."
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            if (contract.pdfStatus == PdfStatus.READY && contract.pdfUrl != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onGeneratePdf,
                    enabled = !isActionLoading,
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Text(text = "  PDF'i Aç / İndir")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Eylem butonları
        if (!isActionLoading) {
            if (canGeneratePdf) {
                Button(
                    onClick = onGeneratePdf,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("PDF Oluştur")
                }
            }

            if (canApprove) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sözleşmeyi Onayla")
                }
            }

            if (canRequestEdit) {
                OutlinedButton(
                    onClick = onRequestEdit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Düzenleme Talep Et")
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun EditRequestDialog(
    onConfirm: (note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Düzenleme Talebi") },
        text = {
            Column {
                Text("Hangi alanların düzenlenmesini istiyorsunuz? Lütfen açıklayın.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Açıklama") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(note) },
                enabled = note.isNotBlank(),
            ) {
                Text("Gönder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        },
    )
}

// --- Yardımcı extension'lar ---

@Composable
private fun ContractStatus.toDisplayName(): String = when (this) {
    ContractStatus.DRAFT -> "Taslak"
    ContractStatus.AWAITING_PARTY2 -> "İkinci Taraf Bekleniyor"
    ContractStatus.AWAITING_APPROVAL -> "Onay Bekleniyor"
    ContractStatus.EDIT_REQUESTED -> "Düzenleme İstendi"
    ContractStatus.BOTH_APPROVED -> "Her İki Taraf Onayladı"
    ContractStatus.LOCKED -> "Kilitlendi"
    ContractStatus.CANCELLED -> "İptal Edildi"
}

@Composable
private fun ContractStatus.toColor() = when (this) {
    ContractStatus.DRAFT -> MaterialTheme.colorScheme.outline
    ContractStatus.AWAITING_PARTY2 -> MaterialTheme.colorScheme.tertiary
    ContractStatus.AWAITING_APPROVAL -> MaterialTheme.colorScheme.primary
    ContractStatus.EDIT_REQUESTED -> MaterialTheme.colorScheme.error
    ContractStatus.BOTH_APPROVED -> MaterialTheme.colorScheme.secondary
    ContractStatus.LOCKED -> MaterialTheme.colorScheme.secondary
    ContractStatus.CANCELLED -> MaterialTheme.colorScheme.error
}