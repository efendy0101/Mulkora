package com.mulkora.feature.settings.presentation.deleteaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Hesap silme onay ekranı.
 * CLAUDE.md §3: Hesap silme geri alınamaz; kullanıcı onay metni girer.
 * Cloud Function üzerinden tüm veriler temizlenir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    onNavigateUp: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: DeleteAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Hesabı Kalıcı Olarak Sil") },
            text = {
                Text(
                    "Bu işlem geri alınamaz. Tüm verileriniz (gönderiler, referanslar, bildirimler) " +
                            "kalıcı olarak silinecektir. Devam etmek istiyor musunuz?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.onDeleteClicked(onAccountDeleted)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hesabı Sil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "⚠ Dikkat: Bu işlem geri alınamaz",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Hesabınızı sildiğinizde:\n" +
                        "• Tüm gönderileriniz silinecek\n" +
                        "• Referans geçmişiniz silinecek\n" +
                        "• Bildirimleriniz silinecek\n" +
                        "• Profil bilgileriniz kaldırılacak\n\n" +
                        "Devam etmek için aşağıya \"${DeleteAccountViewModel.CONFIRMATION_TEXT}\" yazın.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.confirmationText,
                onValueChange = viewModel::onConfirmationTextChange,
                label = { Text("Onay metni") },
                placeholder = { Text(DeleteAccountViewModel.CONFIRMATION_TEXT) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null
            )

            uiState.error?.let { msg ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading &&
                        uiState.confirmationText.trim() == DeleteAccountViewModel.CONFIRMATION_TEXT,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Hesabımı Kalıcı Olarak Sil")
                }
            }
        }
    }
}