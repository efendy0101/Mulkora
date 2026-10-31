package com.mulkora.feature.feed.presentation.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.mulkora.feature.feed.domain.model.Post
import com.mulkora.feature.feed.domain.model.ReactionType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Gönderi tarihi biçimlendirici yardımcı.
 */
private fun Timestamp?.toRelativeTimeString(): String {
    this ?: return ""
    val sdf = SimpleDateFormat("d MMM yyyy", Locale("tr"))
    return sdf.format(toDate())
}

/**
 * Gönderi kartı bileşeni.
 * CLAUDE.md §6: Her kartta profil fotoğrafı, kullanıcı adı, rozet, tarih, kategori, tür,
 * başlık, metin, görsel, beğeni, eksi oy, yorum, puan, kaydet, şikâyet, paylaş gösterilmeli.
 */
@Composable
fun PostCard(
    post: Post,
    onPostClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onReactionClick: (ReactionType) -> Unit,
    onSaveClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPostClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Yazar bilgisi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAuthorClick() }
            ) {
                // Profil fotoğrafı
                AsyncImage(
                    model = post.authorPhotoUrl,
                    contentDescription = "${post.authorDisplayName} profil fotoğrafı",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorDisplayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Kullanıcı adı, rol rozeti ve gönderi tarihi
                    Text(
                        text = buildString {
                            append("@${post.authorUsername} · ${post.authorRole.value}")
                            val dateStr = post.createdAt.toRelativeTimeString()
                            if (dateStr.isNotEmpty()) append(" · $dateStr")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Kaydet butonu
                IconButton(onClick = onSaveClick) {
                    Icon(
                        imageVector = if (post.isSaved)
                            Icons.Filled.Bookmark
                        else
                            Icons.Filled.BookmarkBorder,
                        contentDescription = if (post.isSaved) "Kayıttan kaldır" else "Kaydet",
                        tint = if (post.isSaved)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kategori ve tür chip'leri
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = post.postType.displayName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = post.category.displayName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Başlık
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // İçerik özeti
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gönderi görseli
            if (post.imageUrl != null) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Gönderi görseli",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Aksiyon satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Beğen / Beğenme
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Beğen
                    IconButton(
                        onClick = { onReactionClick(ReactionType.LIKE) }
                    ) {
                        Icon(
                            imageVector = if (post.currentUserReaction == ReactionType.LIKE)
                                Icons.Filled.ThumbUp
                            else
                                Icons.Filled.ThumbUpOffAlt,
                            contentDescription = "Beğen",
                            tint = if (post.currentUserReaction == ReactionType.LIKE)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = post.likeCount.toString(),
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Beğenme
                    IconButton(
                        onClick = { onReactionClick(ReactionType.DISLIKE) }
                    ) {
                        Icon(
                            imageVector = if (post.currentUserReaction == ReactionType.DISLIKE)
                                Icons.Filled.ThumbDown
                            else
                                Icons.Filled.ThumbDownOffAlt,
                            contentDescription = "Beğenme",
                            tint = if (post.currentUserReaction == ReactionType.DISLIKE)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = post.dislikeCount.toString(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Ortalama puan — CLAUDE.md §6 zorunlu gösterim
                if (post.ratingCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Puan",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "%.1f".format(post.averageRating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Yorum sayısı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Yorumlar",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = post.commentCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Paylaş — CLAUDE.md §6
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Paylaş",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Şikâyet — CLAUDE.md §6
                IconButton(onClick = onReportClick) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Şikâyet et",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
