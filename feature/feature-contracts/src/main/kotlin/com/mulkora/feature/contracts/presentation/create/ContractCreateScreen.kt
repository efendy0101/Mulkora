package com.mulkora.feature.contracts.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Yeni kira sözleşmesi oluşturma formu.
 *
 * CLAUDE.md §14: Sözleşme oluşturma akışı — taşınmaz bilgileri,
 * kira koşulları, dönem ve demirbaşlar girilir; taslak Cloud
 * Function'a (createContract) gönderilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractCreateScreen(
    onNavigateUp: () -> Unit,
    onContractCreated: (contractId: String) -> Unit,
    viewModel: ContractCreateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ContractCreateEvent.ContractCreated -> onContractCreated(event.contractId)
                is ContractCreateEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(uiState.fieldError) {
        uiState.fieldError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFieldError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sözleşme Oluştur") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Taşınmaz Bilgileri ---
            SectionHeader("Taşınmaz Bilgileri")

            OutlinedTextField(
                value = uiState.propertyAddress,
                onValueChange = viewModel::onPropertyAddressChange,
                label = { Text("Adres *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            OutlinedTextField(
                value = uiState.propertyType,
                onValueChange = viewModel::onPropertyTypeChange,
                label = { Text("Tür (örn. Daire, Dükkan) *") },
                modifier = Modifier.fillMaxWidth(),
            )

            // --- Kira Koşulları ---
            SectionHeader("Kira Koşulları")

            OutlinedTextField(
                value = uiState.monthlyRentRaw,
                onValueChange = viewModel::onMonthlyRentChange,
                label = { Text("Aylık Kira (₺) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            OutlinedTextField(
                value = uiState.paymentDayRaw,
                onValueChange = viewModel::onPaymentDayChange,
                label = { Text("Ödeme Günü (1–31) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            OutlinedTextField(
                value = uiState.paymentMethod,
                onValueChange = viewModel::onPaymentMethodChange,
                label = { Text("Ödeme Yöntemi (örn. Havale, EFT, Nakit) *") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.depositAmountRaw,
                onValueChange = viewModel::onDepositAmountChange,
                label = { Text("Depozito (₺) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            OutlinedTextField(
                value = uiState.monthlyFeeRaw,
                onValueChange = viewModel::onMonthlyFeeChange,
                label = { Text("Aylık Aidat (₺) (isteğe bağlı)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            // --- Dönem ---
            SectionHeader("Kira Dönemi")

            OutlinedTextField(
                value = uiState.startDate,
                onValueChange = viewModel::onStartDateChange,
                label = { Text("Başlangıç Tarihi (GG.AA.YYYY) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            OutlinedTextField(
                value = uiState.endDate,
                onValueChange = viewModel::onEndDateChange,
                label = { Text("Bitiş Tarihi (GG.AA.YYYY) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            // --- Demirbaşlar ---
            SectionHeader("Demirbaşlar (isteğe bağlı)")

            InventoryInputSection(
                items = uiState.inventoryItems,
                onAdd = viewModel::onInventoryItemAdd,
                onRemove = viewModel::onInventoryItemRemove,
            )

            // --- Özel Şartlar ---
            SectionHeader("Özel Şartlar (isteğe bağlı)")

            OutlinedTextField(
                value = uiState.specialConditions,
                onValueChange = viewModel::onSpecialConditionsChange,
                label = { Text("Özel şartlar veya notlar") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Taslak Oluştur")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun InventoryInputSection(
    items: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var currentItem by rememberSaveable { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = currentItem,
            onValueChange = { currentItem = it },
            label = { Text("Demirbaş ekle") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        IconButton(
            onClick = {
                onAdd(currentItem)
                currentItem = ""
            },
            enabled = currentItem.isNotBlank(),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ekle")
        }
    }

    if (items.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEach { item ->
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(item) },
                    trailingIcon = {
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kaldır",
                            )
                        }
                    },
                )
            }
        }
    }
}