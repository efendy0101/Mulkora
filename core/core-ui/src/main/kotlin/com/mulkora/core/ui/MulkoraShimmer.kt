package com.mulkora.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Shimmer (yükleme iskelet) efekti.
 *
 * docs/22-development-phases.md Faz 13: shimmer efekti.
 * CLAUDE.md §28: Yükleme sırasında kullanıcı deneyimini iyileştirir.
 */

/**
 * Shimmer Modifier extension. Herhangi bir Box/Surface'e uygulanabilir.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(0f, 0f),
            end = Offset(Float.MAX_VALUE, Float.MAX_VALUE),
        ),
    )
}

/**
 * Gönderi kartı shimmer yer tutucusu.
 * Feed yüklenirken kullanılır.
 */
@Composable
fun PostCardShimmer(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Avatar + kullanıcı adı satırı
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .shimmerEffect(),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Başlık
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // İçerik satırları
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (it == 2) 0.6f else 1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Aksiyon satırı
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect(),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect(),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect(),
            )
        }
    }
}