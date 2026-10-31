package com.mulkora.feature.auth.presentation.emailverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * E-posta doğrulama ekranı.
 * CLAUDE.md §2: Kayıt sonrası e-posta doğrulama zorunludur.
 * - Kullanıcıya e-posta gönderildiği bildirilir.
 * - "Doğrulandı" butonuyla Firebase token yenilenerek kontrol yapılır.
 * - Yeniden gönder: 60 sn cooldown
 */
@Composable
fun EmailVerificationScreen(
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MarkEmailRead,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "E-postanızı Doğrulayın",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Kayıt e-postanıza bir doğrulama bağlantısı gönderdik. Bağlantıya tıkladıktan sonra aşağıdaki düğmeye basın.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.onCheckVerification(onVerified) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isChecking && !uiState.isSending
            ) {
                if (uiState.isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("E-postamı Doğruladım")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.onResendClicked() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.resendCooldown == 0 && !uiState.isSending && !uiState.isChecking
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (uiState.resendCooldown > 0) {
                    Text("Yeniden Gönder (${uiState.resendCooldown}s)")
                } else {
                    Text("Yeniden Gönder")
                }
            }

            uiState.message?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (msg.contains("gönderildi"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { viewModel.onSignOutClicked(onBackToLogin) }
            ) {
                Text(
                    "Farklı bir hesapla giriş yap",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}