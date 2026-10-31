package com.mulkora.feature.postcreate.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType

/**
 * Gönderi oluşturma ekranı.
 *
 * CLAUDE.md §7: Gönderi oluşturma kuralları.
 * CLAUDE.md §16: Görseller ExifStripper ile EXIF temizlendikten sonra yüklenir.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onNavigateUp: () -> Unit,
    onPostCreated: (postId: String) -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigasyon eventi — ViewModel API: onNavigationHandled()
    LaunchedEffect(uiState.navigationEvent) {
        uiState.navigationEvent?.let { event ->
            when (event) {
                is CreatePostNavigationEvent.PostCreated -> {
                    onPostCreated(event.postId)
                    viewModel.onNavigationHandled()
                }
            }
        }
    }

    // Hata mesajı — ViewModel state: generalError
    LaunchedEffect(uiState.generalError) {
        uiState.generalError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // Görsel seçici — EXIF temizleme context ile yapılır
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            // CLAUDE.md §16: EXIF temizleme ViewModel üzerinden IO thread'de yapılır
            viewModel.onImageSelectedWithContext(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gönderi Oluştur") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                        )
                    }
                },
                actions = {
                    val isBusy = uiState.isLoading || uiState.isProcessingImage
                    if (!isBusy) {
                        Button(
                            onClick = viewModel::onSubmitPost,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text("Yayınla")
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ── Başlık ────────────────────────────────────────────────
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Başlık *") },
                    placeholder = { Text("10-200 karakter") },
                    isError = uiState.titleError != null,
                    supportingText = uiState.titleError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )

                // ── Gönderi Türü ──────────────────────────────────────────
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = postTypeLabel(uiState.selectedPostType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gönderi Türü *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                    ) {
                        PostType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(postTypeLabel(type)) },
                                onClick = {
                                    viewModel.onPostTypeSelected(type)
                                    typeExpanded = false
                                },
                            )
                        }
                    }
                }

                // ── Kategori ──────────────────────────────────────────────
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = postCategoryLabel(uiState.selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        PostCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(postCategoryLabel(cat)) },
                                onClick = {
                                    viewModel.onCategorySelected(cat)
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }

                // ── İçerik ────────────────────────────────────────────────
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    label = { Text("İçerik *") },
                    placeholder = { Text("20-5000 karakter") },
                    isError = uiState.contentError != null,
                    supportingText = uiState.contentError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 15,
                )

                // ── Etiketler ─────────────────────────────────────────────
                Text(
                    text = "Etiketler (maks. ${CreatePostViewModel.MAX_TAGS})",
                    style = MaterialTheme.typography.labelMedium,
                )
                var tagInput by remember { mutableStateOf("") }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Etiket ekle") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    IconButton(
                        onClick = {
                            if (tagInput.isNotBlank()) {
                                viewModel.onTagAdded(tagInput.trim())
                                tagInput = ""
                            }
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Etiket ekle")
                    }
                }
                uiState.tagsError?.let { errMsg ->
                    Text(
                        text = errMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.onTagRemoved(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Etiketi kaldır",
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }

                // ── Görsel ────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.selectedImageUri != null,
                        onClick = {
                            mediaLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        label = {
                            Text(
                                if (uiState.selectedImageUri != null) "Görsel eklendi"
                                else "Görsel ekle",
                            )
                        },
                        leadingIcon = {
                            if (uiState.isProcessingImage) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                    )
                    if (uiState.selectedImageUri != null) {
                        IconButton(onClick = viewModel::onImageRemoved) {
                            Icon(Icons.Default.Close, contentDescription = "Görseli kaldır")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Yardımcı etiket fonksiyonları ────────────────────────────────────────────
// Enum'ların kendi displayName'lerini kullanır; yeni değer eklendiğinde güncelleme gerekmez.

private fun postTypeLabel(type: PostType): String = type.displayName

private fun postCategoryLabel(category: PostCategory): String = category.displayName
