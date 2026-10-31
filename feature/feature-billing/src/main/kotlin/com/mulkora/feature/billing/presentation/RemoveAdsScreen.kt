package com.mulkora.feature.billing.presentation

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mulkora.feature.billing.domain.model.PurchaseStatus

/**
 * SCR-080 — Reklamları Kaldır satın alma ekranı.
 *
 * CLAUDE.md §33: Compose ekranları doğrudan Billing API'ya erişemez;
 * tüm satın alma işlemleri [BillingViewModel] üzerinden yönetilir.
 *
 * CLAUDE.md §18:
 * - "Satın Almaları Geri Yükle" seçeneği bulunur.
 * - Satın alma yapıldıktan sonra onay ekranına geçilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveAdsScreen(
    onNavigateUp: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Activity referansını BillingRepository'ye bağla (launchBillingFlow için gerekli)
    val context = LocalContext.current
    val activity = remember(context) { context as? android.app.Activity }
    DisposableEffect(activity) {
        viewModel.setActivity(activity)
        onDispose { viewModel.setActivity(null) }
    }

    // Satın alma başarılı ise onay ekranına yönlendir
    LaunchedEffect(uiState.purchaseStatus) {
        if (uiState.purchaseStatus == PurchaseStatus.PURCHASED) {
            onPurchaseSuccess()
        }
    }

    // Hata mesajı göster
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reklamları Kaldır") },
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                RemoveAdsContent(
                    purchaseStatus = uiState.purchaseStatus,
                    onPurchaseClick = { viewModel.purchaseRemoveAds() },
                    onRestoreClick = { viewModel.restorePurchases() },
                )
            }
        }
    }
}

@Composable
private fun RemoveAdsContent(
    purchaseStatus: PurchaseStatus,
    onPurchaseClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (purchaseStatus == PurchaseStatus.PURCHASED) {
            // Zaten satın alınmış durumu
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Reklamlar Kaldırıldı",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MülkOra'yı reklamsız kullanıyorsunuz. Teşekkürler!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Satın alma teklifi
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Reklamsız Deneyim",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tek seferlik ödemeyle MülkOra'daki tüm reklamları kalıcı olarak kaldırın.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onPurchaseClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reklamları Kaldır")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRestoreClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Satın Almaları Geri Yükle")
            }
        }
    }
}