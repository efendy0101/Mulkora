package com.mulkora.feature.explore.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mulkora.core.common.model.PostCategory

/**
 * Keşfet ana ekranı.
 *
 * SCR-030: Kategori ve şehir filtreleri ile gönderi arama.
 * CLAUDE.md §7: Keşfet — Kategori, şehir ve serbest metin araması.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToCategoryPosts: (PostCategory) -> Unit,
    onNavigateToCityPosts: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keşfet") },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // Arama çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Şehir, kategori veya konu ara…") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Kategori başlığı
            Text(
                text = "Kategoriye Göre",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Kategori ızgarası
            val filteredCategories = PostCategory.entries.filter { category ->
                searchQuery.isBlank() ||
                    category.displayName.contains(searchQuery, ignoreCase = true)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(filteredCategories) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onNavigateToCategoryPosts(category) },
                    )
                }

                // Şehir araması kartı
                item {
                    CitySearchCard(
                        onClick = {
                            // Şehir seçimi için filtre açılır; şimdilik İstanbul ile başlat
                            onNavigateToCityPosts("istanbul")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: PostCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun CitySearchCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.LocationCity,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Şehre Göre",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/**
 * PostCategory için görüntüleme adı ve ikon.
 * CLAUDE.md §7: Kategori listesi sabit tanımlı.
 */
private val PostCategory.icon: ImageVector
    get() = when (this) {
        PostCategory.RENT_INCREASE -> Icons.Default.MonetizationOn
        PostCategory.DEPOSIT -> Icons.Default.Home
        PostCategory.DUES -> Icons.Default.Apartment
        PostCategory.HOME_MAINTENANCE -> Icons.Default.Build
        PostCategory.RENOVATION -> Icons.Default.Build
        PostCategory.NOISE -> Icons.Default.Info
        PostCategory.NEIGHBORHOOD -> Icons.Default.LocationCity
        PostCategory.EVICTION -> Icons.Default.Info
        PostCategory.LEASE_CONTRACT -> Icons.Default.Info
        PostCategory.COMMUNICATION -> Icons.Default.Info
        PostCategory.BILLS -> Icons.Default.MonetizationOn
        PostCategory.MOVING -> Icons.Default.Home
        PostCategory.DAMAGE_MAINTENANCE -> Icons.Default.Build
        PostCategory.GENERAL -> Icons.Default.Category
    }
