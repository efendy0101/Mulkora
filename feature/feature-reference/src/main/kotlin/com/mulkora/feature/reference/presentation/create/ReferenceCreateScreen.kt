package com.mulkora.feature.reference.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.feature.reference.domain.model.ReferenceType
import com.mulkora.feature.reference.domain.model.RelationshipType

/**
 * Referans oluşturma ekranı.
 * CLAUDE.md §13: Referans başlatma Cloud Functions üzerinden yapılır.
 * initiateReference CF çağrısı yapılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceCreateScreen(
    onNavigateUp: () -> Unit,
    onReferenceCreated: (referenceId: String) -> Unit,
    viewModel: ReferenceCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referans Oluştur") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Referans Türü
                ReferenceTypeDropdown(
                    selected = uiState.referenceType,
                    onSelect = viewModel::onReferenceTypeSelected
                )

                // İlişki Türü
                RelationshipTypeDropdown(
                    selected = uiState.relationshipType,
                    onSelect = viewModel::onRelationshipTypeSelected
                )

                // Puan (1-5 yıldız)
                Column {
                    Text(
                        text = "Puan",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StarRatingRow(
                        rating = uiState.rating,
                        onRatingChanged = viewModel::onRatingChanged
                    )
                }

                // Referans içeriği
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChanged,
                    label = { Text("Referans Metni") },
                    placeholder = { Text("En az 20 karakter yazınız...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 8,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uiState.contentError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${uiState.content.length}/500",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    isError = uiState.contentError != null
                )

                // İsteğe bağlı mülk adresi
                OutlinedTextField(
                    value = uiState.propertyAddress,
                    onValueChange = viewModel::onPropertyAddressChanged,
                    label = { Text("Mülk Adresi (İsteğe Bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Başlangıç / Bitiş tarihi (metin olarak — ileride DatePicker entegre edilebilir)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.startDate,
                        onValueChange = viewModel::onStartDateChanged,
                        label = { Text("Başlangıç (GG.AA.YYYY)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.endDate,
                        onValueChange = viewModel::onEndDateChanged,
                        label = { Text("Bitiş (GG.AA.YYYY)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                uiState.generalError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        viewModel.onSubmit(onReferenceCreated)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Referans Gönder")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceTypeDropdown(
    selected: ReferenceType?,
    onSelect: (ReferenceType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Referans Türü") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ReferenceType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelationshipTypeDropdown(
    selected: RelationshipType?,
    onSelect: (RelationshipType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("İlişki Türü") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RelationshipType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StarRatingRow(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { star ->
            IconButton(
                onClick = { onRatingChanged(star) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = "$star yıldız",
                    tint = if (star <= rating) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}