package com.mulkora.feature.notifications.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.feature.notifications.domain.model.AppNotification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateToDeepLink: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bildirimler") },
                actions = {
                    if (uiState.notifications.any { !it.isRead }) {
                        IconButton(onClick = viewModel::markAllAsRead) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Tümünü okundu işaretle")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.notifications.isEmpty() -> {
                    EmptyNotificationsPlaceholder(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = uiState.notifications,
                            key = { it.id },
                        ) { notification ->
                            SwipeableNotificationItem(
                                notification = notification,
                                onDismiss = { viewModel.deleteNotification(notification.id) },
                                onClick = {
                                    if (!notification.isRead) viewModel.markAsRead(notification.id)
                                    notification.deepLink?.let(onNavigateToDeepLink)
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationItem(
    notification: AppNotification,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            colors = ListItemDefaults.colors(
                containerColor = if (!notification.isRead)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface,
            ),
            leadingContent = {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (!notification.isRead)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            headlineContent = {
                Text(
                    text = notification.title,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                    notification.createdAt?.let { timestamp ->
                        Text(
                            text = formatNotificationTime(timestamp.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun EmptyNotificationsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = "Henüz bildirim yok",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Referans, gönderi ve diğer aktiviteler burada görünecek.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

private fun formatNotificationTime(date: java.util.Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time
    return when {
        diff < 60_000 -> "Az önce"
        diff < 3_600_000 -> "${diff / 60_000} dk önce"
        diff < 86_400_000 -> "${diff / 3_600_000} saat önce"
        diff < 604_800_000 -> "${diff / 86_400_000} gün önce"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale("tr"))
            sdf.format(date)
        }
    }
}