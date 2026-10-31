package com.mulkora.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Yuvarlak kullanıcı avatarı bileşeni.
 *
 * Fotoğraf URL'si boş veya yüklenemezse görünen adın baş harfini
 * renkli bir daire içinde gösterir.
 *
 * CLAUDE.md §28: Erişilebilirlik — contentDescription zorunlu.
 */
@Composable
fun UserAvatarImage(
    photoUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val initial = displayName
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    if (!photoUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "$displayName profil fotoğrafı",
            contentScale = ContentScale.Crop,
            loading = {
                AvatarInitial(initial = initial, size = size)
            },
            error = {
                AvatarInitial(initial = initial, size = size)
            },
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        AvatarInitial(
            initial = initial,
            size = size,
            modifier = modifier,
        )
    }
}

@Composable
private fun AvatarInitial(
    initial: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}