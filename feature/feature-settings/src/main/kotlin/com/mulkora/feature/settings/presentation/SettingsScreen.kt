package com.mulkora.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Ayarlar ana ekranı.
 *
 * SCR-070: Ayarlar ekranı.
 * CLAUDE.md §20: Tüm bölümler (Hesap, Gizlilik, Reklam ve Satın Alma,
 * Uygulama, Bilgilendirme) gösterilmelidir.
 * CLAUDE.md §33: TODO bırakma.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {},
    onNavigateToTermsOfService: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToKvkk: () -> Unit = {},
    onNavigateToCommunityGuidelines: () -> Unit = {},
    onNavigateToRemoveAds: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToHelpSupport: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToBlockedUsers: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.navigationEvent) {
        when (uiState.navigationEvent) {
            SettingsNavigationEvent.NavigateToLogin -> {
                onNavigateToLogin()
                viewModel.onNavigationHandled()
            }
            null -> Unit
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ayarlar") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Hesap ──────────────────────────────────────────────────
                SettingsSectionHeader("Hesap")

                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Profili Düzenle",
                    onClick = onNavigateToProfile,
                )
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "E-posta Doğrulama Gönder",
                    subtitle = "Hesabınızı doğrulamak için e-posta alın.",
                    onClick = viewModel::onSendEmailVerificationClick,
                )
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Şifre Değiştir",
                    onClick = onNavigateToChangePassword,
                )

                HorizontalDivider()
                // ── Gizlilik ───────────────────────────────────────────────
                SettingsSectionHeader("Gizlilik")

                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Bildirim Tercihleri",
                    subtitle = "Hangi bildirimleri alacağınızı yönetin.",
                    onClick = onNavigateToNotificationSettings,
                )
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = "Engellenen Kullanıcılar",
                    onClick = onNavigateToBlockedUsers,
                )
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Verilerimi İndir",
                    subtitle = "Kişisel verilerinizin bir kopyasını talep edin.",
                    onClick = viewModel::onRequestDataExportClick,
                )

                HorizontalDivider()
                // ── Reklam ve Satın Alma ────────────────────────────────────
                SettingsSectionHeader("Reklam ve Satın Alma")

                SettingsItem(
                    icon = Icons.Default.RemoveCircle,
                    title = "Reklamları Kaldır",
                    subtitle = "Tek seferlik satın alma ile tüm reklamları kapatın.",
                    onClick = onNavigateToRemoveAds,
                )
                SettingsItem(
                    icon = Icons.Default.ShoppingCart,
                    title = "Satın Almaları Geri Yükle",
                    subtitle = "Önceki satın almalarınızı bu cihaza yükleyin.",
                    onClick = viewModel::onRestorePurchasesClick,
                )

                HorizontalDivider()
                // ── Bilgilendirme ───────────────────────────────────────────
                SettingsSectionHeader("Bilgilendirme")

                SettingsItem(
                    icon = Icons.Default.Gavel,
                    title = "Kullanım Koşulları",
                    onClick = onNavigateToTermsOfService,
                )
                SettingsItem(
                    icon = Icons.Default.Policy,
                    title = "Gizlilik Politikası",
                    onClick = onNavigateToPrivacyPolicy,
                )
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "KVKK Aydınlatma Metni",
                    onClick = onNavigateToKvkk,
                )
                SettingsItem(
                    icon = Icons.Default.StarRate,
                    title = "Topluluk Kuralları",
                    onClick = onNavigateToCommunityGuidelines,
                )
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Uygulama Hakkında",
                    onClick = onNavigateToAbout,
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Yardım ve Destek",
                    onClick = onNavigateToHelpSupport,
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Sıkça Sorulan Sorular",
                    onClick = onNavigateToFaq,
                )

                HorizontalDivider()
                // ── Hesap İşlemleri ─────────────────────────────────────────
                SettingsSectionHeader("Hesap İşlemleri")

                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Çıkış Yap",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = viewModel::onSignOutClick,
                )
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Hesabı Sil",
                    subtitle = "Tüm verileriniz kalıcı olarak silinir.",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onNavigateToDeleteAccount,
                )

                // ── Sürüm ───────────────────────────────────────────────────
                // CLAUDE.md §20: Sürüm numarası sabit metin olmamalı, otomatik alınmalı.
                val packageInfo = remember {
                    runCatching {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }.getOrNull()
                }
                val versionName = packageInfo?.versionName ?: "–"

                Text(
                    text = "Sürüm $versionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint,
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint,
            )
        },
        supportingContent = subtitle?.let { sub ->
            { Text(sub, style = MaterialTheme.typography.bodySmall) }
        },
    )
}
