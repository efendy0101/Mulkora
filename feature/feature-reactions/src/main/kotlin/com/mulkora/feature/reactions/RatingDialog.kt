package com.mulkora.feature.reactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Gönderi puanlama iletişim kutusu (1-5 yıldız).
 *
 * CLAUDE.md §9: Her gönderi 1-5 yıldız arası puanlanabilir.
 * Kullanıcı gönderi başına yalnızca bir kez puan verebilir.
 * Mevcut puan varsa dialog başlangıçta o değeri gösterir.
 *
 * @param initialRating Kullanıcının mevcut puanı (1-5) veya null
 * @param onConfirm Seçilen puan (1-5) ile çağrılır
 * @param onDismiss Dialog kapatıldığında çağrılır
 */
@Composable
fun RatingDialog(
    postTitle: String = "",
    initialRating: Int? = null,
    currentRating: Int = 0,
    onConfirm: (rating: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // currentRating parametresi PostReactionBar'dan gelir (initialRating yoksa fallback)
    var selectedRating by remember { mutableIntStateOf(initialRating ?: currentRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Gönderiyi Puanla",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = postTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PostRatingBar(
                    rating = selectedRating,
                    onRatingChange = { selectedRating = it },
                )
                Spacer(modifier = Modifier.height(8.dp))
                val label = when (selectedRating) {
                    1 -> "Çok kötü"
                    2 -> "Kötü"
                    3 -> "Orta"
                    4 -> "İyi"
                    5 -> "Çok iyi"
                    else -> "Puan seçin"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedRating > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selectedRating > 0) onConfirm(selectedRating) },
                enabled = selectedRating > 0,
            ) {
                Text("Onayla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        },
    )
}

/**
 * 5 yıldızlı puanlama satırı bileşeni.
 *
 * Hem okuma hem yazma modunda çalışır.
 *
 * @param rating Mevcut puan (0 = puan yok)
 * @param onRatingChange null ise salt okunur
 * @param starSize Yıldız boyutu
 */
@Composable
fun PostRatingBar(
    rating: Int,
    modifier: Modifier = Modifier,
    onRatingChange: ((Int) -> Unit)? = null,
    starSize: Int = 36,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (star in 1..5) {
            val filled = star <= rating
            if (onRatingChange != null) {
                IconButton(
                    onClick = { onRatingChange(star) },
                    modifier = Modifier.size(starSize.dp),
                ) {
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "$star yıldız",
                        tint = if (filled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size((starSize - 4).dp),
                    )
                }
            } else {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (filled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(starSize.dp),
                )
            }
        }
    }
}