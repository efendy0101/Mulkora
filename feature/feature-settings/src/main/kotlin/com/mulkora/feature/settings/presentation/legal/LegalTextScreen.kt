package com.mulkora.feature.settings.presentation.legal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Genel amaçlı hukuki metin ekranı.
 *
 * CLAUDE.md §18: Kullanıcı Sözleşmesi, Gizlilik Politikası, KVKK Metni ve Topluluk
 * Kuralları ekranlarında aynı yapı kullanılır; içerik parametre olarak verilir.
 *
 * @param title  Ekran başlığı (örn. "Kullanıcı Sözleşmesi")
 * @param body   Gösterilecek tam metin
 * @param onBack Geri tuşu/ikon tıklaması
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalTextScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Sabit metin yer tutucular — gerçek içerik uygulama release'i öncesinde
// hukuk danışmanı tarafından sağlanmalıdır.
// ---------------------------------------------------------------------------

const val TERMS_OF_SERVICE_TEXT = """
MÜLKOra KULLANICI SÖZLEŞMESİ

Bu sözleşme, MülkOra uygulamasını kullanan kişiler ile MülkOra arasındaki hak ve yükümlülükleri düzenler.

[İçerik hazırlanmaktadır — lütfen daha sonra tekrar kontrol edin.]
"""

const val PRIVACY_POLICY_TEXT = """
MÜLKOra GİZLİLİK POLİTİKASI

Kişisel verilerinizin nasıl toplandığı, kullanıldığı ve korunduğu bu belgede açıklanmaktadır.

[İçerik hazırlanmaktadır — lütfen daha sonra tekrar kontrol edin.]
"""

const val KVKK_TEXT = """
KİŞİSEL VERİLERİN KORUNMASI KANUNU (KVKK) KAPSAMINDA AYDINLATMA METNİ

6698 sayılı Kişisel Verilerin Korunması Kanunu uyarınca hazırlanan aydınlatma metnidir.

[İçerik hazırlanmaktadır — lütfen daha sonra tekrar kontrol edin.]
"""

const val COMMUNITY_GUIDELINES_TEXT = """
MÜLKOra TOPLULUK KURALLARI

Platformumuzda sağlıklı bir ortam sağlamak için aşağıdaki kurallara uyulması zorunludur.

[İçerik hazırlanmaktadır — lütfen daha sonra tekrar kontrol edin.]
"""