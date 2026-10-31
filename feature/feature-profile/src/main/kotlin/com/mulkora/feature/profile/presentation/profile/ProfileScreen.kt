package com.mulkora.feature.profile.presentation.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToReferences: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.profile?.username?.let { "@$it" } ?: "Profil",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                actions = {
                    if (uiState.isCurrentUser) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                        }
                    } else {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Daha fazla")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Kullanıcıyı Engelle") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    uiState.profile?.uid?.let { viewModel.blockUser(it) }
                                },
                            )
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
                uiState.profile == null -> {
                    Text(
                        text = "Profil bulunamadı.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                else -> {
                    val profile = uiState.profile!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Profil fotoğrafı
                        Surface(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            AsyncImage(
                                model = profile.photoUrl,
                                contentDescription = "Profil fotoğrafı",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Görünen ad + doğrulama
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            if (profile.isEmailVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = "E-posta doğrulandı",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        // Rol rozeti
                        Text(
                            text = profile.role.name.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        // Şehir
                        profile.city?.let { city ->
                            Text(
                                text = city,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Biyografi
                        profile.bio?.let { bio ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // İstatistikler
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatItem(
                                label = "Gönderi",
                                value = profile.postCount.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            StatItem(
                                label = "Referans",
                                value = profile.referenceCount.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            if (profile.referenceCount > 0) {
                                StatItem(
                                    label = "Puan",
                                    value = "%.1f".format(profile.averageReferenceRating),
                                    icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Aksiyon butonları
                        if (uiState.isCurrentUser) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = onNavigateToEditProfile,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Profili Düzenle")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Referanslar butonu
                        if (profile.referenceCount > 0 || uiState.isCurrentUser) {
                            Button(
                                onClick = onNavigateToReferences,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Referansları Gör (${profile.referenceCount})")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}