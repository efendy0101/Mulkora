package com.mulkora.feature.onboarding.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Onboarding ekranı — uygulamanın temel özelliklerini tanıtır.
 * CLAUDE.md §5 Navigasyon: ilk açılışta gösterilir, sonrasında atlanabilir.
 * CLAUDE.md §20: Tamamlanma bayrağı DataStore'a kaydedilir.
 *
 * [onFinish] çağrıldığında navigasyon katmanı kullanıcıyı Login'e yönlendirir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pages = onboardingPages()
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    // Onboarding tamamlandı bayrağını kaydet ve navigasyona bildir
    val handleFinish: () -> Unit = {
        viewModel.markOnboardingCompleted()
        onFinish()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Atla düğmesi — son sayfada gizlenir
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < pages.lastIndex) {
                TextButton(onClick = handleFinish) {
                    Text(text = "Atla")
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingPage(page = pages[pageIndex])
        }

        // Sayfa göstergesi (dots)
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (selected) 10.dp else 8.dp)
                        .then(
                            Modifier.size(8.dp)
                        )
                )
            }
        }

        // İleri / Başla butonu
        Button(
            onClick = {
                if (pagerState.currentPage < pages.lastIndex) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    handleFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = if (pagerState.currentPage < pages.lastIndex) "İleri" else "Başla"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPageData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // İkon/görsel alanı — ileride Lottie animasyonu ile değiştirilebilir
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = page.emoji,
                style = MaterialTheme.typography.displayLarge
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class OnboardingPageData(
    val emoji: String,
    val title: String,
    val description: String
)

private fun onboardingPages(): List<OnboardingPageData> = listOf(
    OnboardingPageData(
        emoji = "🏠",
        title = "Mülkora'ya Hoş Geldiniz",
        description = "Kiracı ve ev sahibi deneyimlerinizi paylaşın," +
                " doğru kişilerle bağlantı kurun."
    ),
    OnboardingPageData(
        emoji = "⭐",
        title = "Referans Sistemi",
        description = "Güvenilir kiracı veya ev sahiplerini referans ile" +
                " onaylayın. Şeffaf ilişkiler kurun."
    ),
    OnboardingPageData(
        emoji = "📄",
        title = "Dijital Kira Sözleşmesi",
        description = "Kira sözleşmelerinizi uygulama üzerinden oluşturun," +
                " onaylayın ve PDF olarak saklayın."
    ),
    OnboardingPageData(
        emoji = "🔔",
        title = "Anlık Bildirimler",
        description = "Referans taleplerinizi ve sözleşme güncellemelerinizi" +
                " anında takip edin."
    )
)