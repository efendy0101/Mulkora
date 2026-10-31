package com.mulkora.feature.reactions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mulkora.feature.reactions.RatingDialog
import com.mulkora.feature.reactions.domain.model.PostReaction
import com.mulkora.feature.reactions.domain.model.ReactionType

/**
 * Gönderi reaksiyon çubuğu — beğeni, eksi oy ve puan.
 *
 * CLAUDE.md §10: Bir kullanıcı aynı gönderiye aynı anda hem beğeni hem eksi oy veremez.
 * Toggle mantığı (aynı tepki → kaldır) Cloud Function tarafında yönetilir.
 *
 * Kullanım:
 * ```
 * PostReactionBar(
 *     reaction = uiState.reaction,
 *     onLike = { viewModel.react(ReactionType.LIKE) },
 *     onDislike = { viewModel.react(ReactionType.DISLIKE) },
 *     onRate = { rating -> viewModel.rate(rating) },
 * )
 * ```
 */
@Composable
fun PostReactionBar(
    reaction: PostReaction?,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var showRatingDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // --- Beğeni ---
        IconButton(
            onClick = onLike,
            enabled = enabled,
        ) {
            Icon(
                imageVector = if (reaction?.userReaction == ReactionType.LIKE)
                    Icons.Filled.ThumbUp
                else
                    Icons.Outlined.ThumbUp,
                contentDescription = "Beğen",
                tint = if (reaction?.userReaction == ReactionType.LIKE)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = (reaction?.likeCount ?: 0).toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // --- Eksi oy ---
        IconButton(
            onClick = onDislike,
            enabled = enabled,
        ) {
            Icon(
                imageVector = if (reaction?.userReaction == ReactionType.DISLIKE)
                    Icons.Filled.ThumbDown
                else
                    Icons.Outlined.ThumbDown,
                contentDescription = "Beğenme",
                tint = if (reaction?.userReaction == ReactionType.DISLIKE)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = (reaction?.dislikeCount ?: 0).toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(16.dp))

        // --- Yıldız puanı ---
        IconButton(
            onClick = { showRatingDialog = true },
            enabled = enabled,
        ) {
            Icon(
                imageVector = if (reaction?.userRating != null)
                    Icons.Filled.Star
                else
                    Icons.Outlined.StarOutline,
                contentDescription = "Puan ver",
                tint = if (reaction?.userRating != null)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (reaction?.ratingCount != null && reaction.ratingCount > 0) {
            Text(
                text = "%.1f".format(reaction.averageRating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showRatingDialog) {
        RatingDialog(
            currentRating = reaction?.userRating ?: 0,
            onDismiss = { showRatingDialog = false },
            onConfirm = { rating ->
                showRatingDialog = false
                onRate(rating)
            },
        )
    }
}