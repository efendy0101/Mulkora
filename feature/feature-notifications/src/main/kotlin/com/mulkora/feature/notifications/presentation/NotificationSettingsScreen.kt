package com.mulkora.feature.notifications.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Bildirim ayarları ekranı (SCR-134).
 *
 * docs/19-notification-system.md: Kullanıcı bildirim tercihlerini yönetir.
 * CLAUDE.md §14: Her bildirim türü ayrı ayrı kapatılabilir.
 *
 * Tercihler Firestore'daki users/{uid}/notificationPreferences belgesine kaydedilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bildirim Tercihleri") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Gönderi Bildirimleri")

            NotificationToggleItem(
                title = "Gönderim Durumu",
                subtitle = "Gönderiniz onaylandığında veya reddedildiğinde.",
                checked = uiState.postStatusEnabled,
                onCheckedChange = viewModel::onPostStatusToggle,
            )

            HorizontalDivider()
            SectionHeader("Yorum Bildirimleri")

            NotificationToggleItem(
                title = "Yeni Yorumlar",
                subtitle = "Gönderinize biri yorum yaptığında.",
                checked = uiState.newCommentEnabled,
                onCheckedChange = viewModel::onNewCommentToggle,
            )
            NotificationToggleItem(
                title = "Yorum Cevapları",
                subtitle = "Yorumunuza biri cevap verdiğinde.",
                checked = uiState.commentReplyEnabled,
                onCheckedChange = viewModel::onCommentReplyToggle,
            )

            HorizontalDivider()
            SectionHeader("Referans Bildirimleri")

            NotificationToggleItem(
                title = "Referans Daveti",
                subtitle = "Biri sizi referans almak istediğinde.",
                checked = uiState.referenceInviteEnabled,
                onCheckedChange = viewModel::onReferenceInviteToggle,
            )
            NotificationToggleItem(
                title = "Referans Durumu",
                subtitle = "Referansınız onaylandığında veya reddedildiğinde.",
                checked = uiState.referenceStatusEnabled,
                onCheckedChange = viewModel::onReferenceStatusToggle,
            )
            NotificationToggleItem(
                title = "Referans Cevabı",
                subtitle = "Referansınıza karşı taraf cevap yazdığında.",
                checked = uiState.referenceResponseEnabled,
                onCheckedChange = viewModel::onReferenceResponseToggle,
            )

            HorizontalDivider()
            SectionHeader("Sözleşme Bildirimleri")

            NotificationToggleItem(
                title = "Sözleşme Daveti",
                subtitle = "Biri sizi sözleşmeye davet ettiğinde.",
                checked = uiState.contractInviteEnabled,
                onCheckedChange = viewModel::onContractInviteToggle,
            )
            NotificationToggleItem(
                title = "Sözleşme Durumu",
                subtitle = "Sözleşmeniz onaylandığında veya kilitlendiğinde.",
                checked = uiState.contractStatusEnabled,
                onCheckedChange = viewModel::onContractStatusToggle,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun NotificationToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}