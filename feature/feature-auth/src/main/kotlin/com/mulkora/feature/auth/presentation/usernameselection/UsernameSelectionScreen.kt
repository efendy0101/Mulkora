package com.mulkora.feature.auth.presentation.usernameselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Kullanıcı adı seçim ekranı.
 * CLAUDE.md §4: Kullanıcı adı benzersizliği backend üzerinden kontrol edilir.
 * Kayıt akışı: RegisterScreen → UsernameSelectionScreen → RoleSelectionScreen → TermsScreen
 */
@Composable
fun UsernameSelectionScreen(
    onContinue: () -> Unit,
    viewModel: UsernameSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Kullanıcı Adı Seç",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Platformdaki kimliğiniz olacak benzersiz bir kullanıcı adı seçin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = viewModel::onUsernameChanged,
                    label = { Text("Kullanıcı adı") },
                    placeholder = { Text("ornek_kullanici") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.availabilityError != null,
                    supportingText = {
                        when {
                            uiState.availabilityError != null ->
                                Text(
                                    text = uiState.availabilityError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            uiState.isAvailable == true ->
                                Text(
                                    text = "@${uiState.username} kullanılabilir!",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            uiState.username.isNotEmpty() && uiState.username.length < 3 ->
                                Text("En az 3 karakter giriniz.")
                            else -> {}
                        }
                    },
                    trailingIcon = {
                        when {
                            uiState.isChecking -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            uiState.isAvailable == true -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Müsait",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            uiState.availabilityError != null -> Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kullanılmıyor",
                                tint = MaterialTheme.colorScheme.error
                            )
                            else -> null
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (uiState.isAvailable == true && !uiState.isLoading) {
                                viewModel.onContinueClicked(onContinue)
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.onContinueClicked(onContinue) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isAvailable == true && !uiState.isLoading && !uiState.isChecking
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Devam Et")
                    }
                }
            }
        }
    }
}