package com.mulkora.feature.reports.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.core.common.model.ReportReason
import com.mulkora.feature.reports.domain.model.ReportTargetType

/**
 * Şikayet oluşturma ekranı.
 * CLAUDE.md Bölüm 11: Şikâyet Sistemi.
 *
 * - Şikayet nedeni seçme (RadioButton listesi).
 * - İsteğe bağlı ek not alanı.
 * - Gönder butonuyla Cloud Function (createReport) çağrısı.
 *
 * @param targetId   Şikayet edilen içeriğin Firestore döküman ID'si.
 * @param targetType Şikayet edilen içerik türü.
 * @param onBack     Geri navigasyon callback'i.
 * @param onSuccess  Başarılı gönderi sonrası navigasyon callback'i.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    targetId: String,
    targetType: ReportTargetType,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Başarılı gönderim → snackbar + navigasyon
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Şikayetiniz alındı. Teşekkür ederiz.")
            onSuccess()
        }
    }

    // Hata mesajı → snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Şikayet Et") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Şikayet nedeninizi seçin",
                style = MaterialTheme.typography.titleMedium,
            )

            // Neden listesi
            Column(modifier = Modifier.selectableGroup()) {
                ReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.selectedReason == reason,
                                onClick = { viewModel.selectReason(reason) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = uiState.selectedReason == reason,
                            onClick = null, // selectable Row yönetiyor
                        )
                        Text(
                            text = reason.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ek not alanı
            OutlinedTextField(
                value = uiState.additionalNote,
                onValueChange = viewModel::onAdditionalNoteChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ek not (isteğe bağlı)") },
                maxLines = 5,
                placeholder = { Text("Açıklamak istediğiniz ek bilgileri buraya yazın…") },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gönder butonu
            Button(
                onClick = { viewModel.submitReport(targetId, targetType) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedReason != null && !uiState.isSubmitting,
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Şikayeti Gönder")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}