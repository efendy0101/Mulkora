package com.mulkora.feature.billing.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Satın alma onayı ekranı.
 *
 * CLAUDE.md §18: "Reklamları Kaldır" satın alma işlemi tamamlandıktan sonra
 * kullanıcıya başarı mesajı gösterilir.
 * docs/20-ads-billing-design.md: PurchaseConfirmation ekranı.
 */
@Composable
fun PurchaseConfirmationScreen(
    onNavigateToFeed: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Satın Alma Başarılı!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Reklamlar kaldırıldı. Artık Mülkora'yı kesintisiz kullanabilirsiniz.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onNavigateToFeed,
            ) {
                Text(text = "Ana Sayfaya Dön")
            }
        }
    }
}