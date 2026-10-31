package com.mulkora.feature.settings.presentation.faq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

/**
 * Sıkça Sorulan Sorular ekranı.
 *
 * CLAUDE.md §20: Bilgilendirme bölümünde "Sıkça Sorulan Sorular" yer alır.
 * SCR-146: SSS ekranı.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    onNavigateUp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sıkça Sorulan Sorular") },
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
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            faqItems.forEachIndexed { index, faq ->
                FaqItem(
                    question = faq.question,
                    answer = faq.answer,
                )
                if (index < faqItems.lastIndex) {
                    HorizontalDivider()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Kapat" else "Genişlet",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private data class FaqEntry(val question: String, val answer: String)

private val faqItems = listOf(
    FaqEntry(
        question = "Mülkora'da nasıl hesap oluşturabilirim?",
        answer = "Uygulamayı indirip açtıktan sonra 'Kayıt Ol' seçeneğine tıklayın. " +
            "E-posta adresinizle ya da Google hesabınızla birkaç adımda kayıt olabilirsiniz.",
    ),
    FaqEntry(
        question = "Referans sistemi nasıl çalışır?",
        answer = "Kiracı veya ev sahibi olarak birlikte yaşadığınız kişi hakkında referans " +
            "oluşturabilirsiniz. Karşı taraf onayladıktan sonra referansınız moderatör " +
            "incelemesine girer. Onaylanan referanslar profilde görünür.",
    ),
    FaqEntry(
        question = "Kira sözleşmesi nasıl oluşturulur?",
        answer = "Profil menüsünden 'Kira Sözleşmelerim'e girin. Yeni sözleşme oluşturun, " +
            "ikinci tarafı davet edin. Her iki taraf onayladıktan sonra PDF olarak indirilebilir.",
    ),
    FaqEntry(
        question = "Gönderilerimi neden göremiyorum?",
        answer = "Yeni gönderiler moderatör onayından sonra yayımlanır. " +
            "Onay bekleme süresi genellikle 24 saat içindedir.",
    ),
    FaqEntry(
        question = "Reklamları nasıl kaldırabilirim?",
        answer = "Ayarlar → 'Reklamları Kaldır' seçeneğinden tek seferlik satın alma " +
            "yaparak tüm reklamları kalıcı olarak kapatabilirsiniz.",
    ),
    FaqEntry(
        question = "Hesabımı nasıl silebilirim?",
        answer = "Ayarlar → 'Hesabı Sil' seçeneğinden hesabınızı kalıcı olarak silebilirsiniz. " +
            "Bu işlem geri alınamaz; tüm verileriniz silinir.",
    ),
    FaqEntry(
        question = "Verilerimi nasıl indirebilirim?",
        answer = "Ayarlar → Gizlilik → 'Verilerimi İndir' seçeneğine tıklayın. " +
            "Verileriniz hazırlandığında e-posta ile bildirim alırsınız.",
    ),
)