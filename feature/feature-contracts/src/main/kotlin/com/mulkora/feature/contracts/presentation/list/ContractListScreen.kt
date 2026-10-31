package com.mulkora.feature.contracts.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mulkora.feature.contracts.domain.model.Contract
import com.mulkora.feature.contracts.domain.model.ContractStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kullanıcının sözleşme listesini gösteren ekran.
 *
 * CLAUDE.md §14: Kiracı ve ev sahibi uygulama üzerinden kira sözleşmesi oluşturabilir,
 * taslak sürümlerini ve onaylanmış sözleşmeleri görebilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractListScreen(
    onCreateContract: () -> Unit,
    onContractClick: (contractId: String) -> Unit,
    viewModel: ContractListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kira Sözleşmelerim") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateContract) {
                Icon(Icons.Default.Add, contentDescription = "Yeni sözleşme oluştur")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.contracts.isEmpty() -> {
                    EmptyContractsPlaceholder(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.contracts,
                            key = { it.id },
                        ) { contract ->
                            ContractListItem(
                                contract = contract,
                                onClick = { onContractClick(contract.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContractListItem(
    contract: Contract,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = contract.propertyAddress.take(40).let {
                        if (contract.propertyAddress.length > 40) "$it…" else it
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                ContractStatusChip(status = contract.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Belge No: ${contract.documentNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${contract.startDate} – ${contract.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (contract.party2DisplayName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Taraf: ${contract.party2DisplayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val dateStr = remember(contract.updatedAt) {
                SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date(contract.updatedAt))
            }
            Text(
                text = "Son güncelleme: $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ContractStatusChip(status: ContractStatus) {
    val (label, color) = when (status) {
        ContractStatus.DRAFT -> "Taslak" to MaterialTheme.colorScheme.outline
        ContractStatus.AWAITING_PARTY2 -> "Davet Bekleniyor" to MaterialTheme.colorScheme.tertiary
        ContractStatus.AWAITING_APPROVAL -> "Onay Bekleniyor" to MaterialTheme.colorScheme.primary
        ContractStatus.EDIT_REQUESTED -> "Düzenleme İstendi" to MaterialTheme.colorScheme.error
        ContractStatus.BOTH_APPROVED -> "Her İki Taraf Onayladı" to MaterialTheme.colorScheme.secondary
        ContractStatus.LOCKED -> "Kilitlendi" to MaterialTheme.colorScheme.secondary
        ContractStatus.CANCELLED -> "İptal Edildi" to MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@Composable
private fun EmptyContractsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Henüz sözleşmeniz yok",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Yeni bir kira sözleşmesi oluşturmak için + butonuna dokunun.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}