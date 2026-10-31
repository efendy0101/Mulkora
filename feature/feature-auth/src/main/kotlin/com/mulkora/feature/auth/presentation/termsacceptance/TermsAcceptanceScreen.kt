package com.mulkora.feature.auth.presentation.termsacceptance

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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * KVKK ve hukuki onaylar ekranı.
 * CLAUDE.md §4: Kayıt sırasında alınması zorunlu onaylar.
 * CLAUDE.md §27: KVKK ve gizlilik kuralları.
 *
 * Tüm onaylar verilmeden "Hesap Oluştur" düğmesi aktif olmaz.
 */
@Composable
fun TermsAcceptanceScreen(
    onComplete: () -> Unit,
    onNavigateToTerms: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToKvkk: () -> Unit = {},
    onNavigateToCommunityRules: () -> Unit = {},
    viewModel: TermsAcceptanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Son Adım",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Hesabınızı oluşturmak için aşağıdaki koşulları onaylayın.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Kullanım Koşulları
            ConsentRow(
                checked = uiState.termsAccepted,
                onCheckedChange = viewModel::onTermsChanged,
                label = buildAnnotatedString {
                    append("Okudum ve ")
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )) { append("Kullanım Koşulları") }
                    append("'nı kabul ediyorum.")
                },
                onLinkClick = onNavigateToTerms
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gizlilik Politikası
            ConsentRow(
                checked = uiState.privacyAccepted,
                onCheckedChange = viewModel::onPrivacyChanged,
                label = buildAnnotatedString {
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )) { append("Gizlilik Politikası") }
                    append("'nı okudum ve onaylıyorum.")
                },
                onLinkClick = onNavigateToPrivacy
            )

            Spacer(modifier = Modifier.height(12.dp))

            // KVKK
            ConsentRow(
                checked = uiState.kvkkAccepted,
                onCheckedChange = viewModel::onKvkkChanged,
                label = buildAnnotatedString {
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )) { append("KVKK Aydınlatma Metni") }
                    append("'ni okudum, kişisel verilerimin işlenmesine rıza veriyorum.")
                },
                onLinkClick = onNavigateToKvkk
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Topluluk Kuralları
            ConsentRow(
                checked = uiState.communityRulesAccepted,
                onCheckedChange = viewModel::onCommunityRulesChanged,
                label = buildAnnotatedString {
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )) { append("Topluluk Kuralları") }
                    append("'nı okudum ve uyacağımı taahhüt ediyorum.")
                },
                onLinkClick = onNavigateToCommunityRules
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 18 Yaş Onayı
            ConsentRow(
                checked = uiState.ageConfirmed,
                onCheckedChange = viewModel::onAgeConfirmedChanged,
                label = buildAnnotatedString {
                    append("18 yaşından büyük olduğumu beyan ediyorum.")
                },
                onLinkClick = null
            )

            Spacer(modifier = Modifier.height(24.dp))

            uiState.error?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.onAcceptAll(onComplete) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.termsAccepted &&
                        uiState.privacyAccepted &&
                        uiState.kvkkAccepted &&
                        uiState.communityRulesAccepted &&
                        uiState.ageConfirmed &&
                        !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Hesap Oluştur")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: androidx.compose.ui.text.AnnotatedString,
    onLinkClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (onLinkClick != null) {
            TextButton(
                onClick = onLinkClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 14.dp)
            )
        }
    }
}