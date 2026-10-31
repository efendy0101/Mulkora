package com.mulkora.feature.settings.presentation.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Uygulama hakkında ekranı.
 *
 * CLAUDE.md §20: Uygulama sürümü sabit metin olmamalı; versionName'den otomatik alınmalıdır.
 * SCR-147: Uygulama Hakkında ekranı.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val packageInfo = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
    }
    val versionName = packageInfo?.versionName ?: "–"
    val versionCode = packageInfo?.let {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            it.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            it.versionCode.toString()
        }
    } ?: "–"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uygulama Hakkında") },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Uygulama logosu yer tutucusu — ileride gerçek logo ile değiştirilecek
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // placeholder
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Mülkora",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Kiracı ve ev sahibinin ortak zemini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            AboutInfoRow(label = "Sürüm", value = versionName)
            Spacer(modifier = Modifier.height(8.dp))
            AboutInfoRow(label = "Derleme", value = versionCode)

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "© 2026 Mülkora. Tüm hakları saklıdır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}