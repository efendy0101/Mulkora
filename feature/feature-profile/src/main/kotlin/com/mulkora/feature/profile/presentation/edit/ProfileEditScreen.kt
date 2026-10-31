package com.mulkora.feature.profile.presentation.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Profil düzenleme ekranı.
 * CLAUDE.md §8: Kullanıcı displayName, bio ve şehir bilgisini günceller.
 * Kullanıcı adı bu ekranda değiştirilemez — bunun için UsernameSelection kullanılır.
 * Rol değişikliği domainin dışındadır, bu ekranda gösterilmez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onNavigateUp: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profili Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Ad Soyad
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = { Text("Ad Soyad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null && uiState.displayName.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hakkımda / Bio
            OutlinedTextField(
                value = uiState.bio,
                onValueChange = viewModel::onBioChange,
                label = { Text("Hakkımda") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("${uiState.bio.length}/${ProfileEditViewModel.BIO_MAX_LENGTH}")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Şehir
            OutlinedTextField(
                value = uiState.city,
                onValueChange = viewModel::onCityChange,
                label = { Text("Şehir") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            uiState.error?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.onSaveClicked(onNavigateUp) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Kaydet")
                }
            }
        }
    }
}