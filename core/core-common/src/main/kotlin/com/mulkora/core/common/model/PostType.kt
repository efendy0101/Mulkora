package com.mulkora.core.common.model

/**
 * Gönderi türleri.
 * CLAUDE.md Bölüm 7: Gönderi Oluşturma
 *
 * Firestore'daki posts/{postId}.postType alanı ile eşleşir.
 */
enum class PostType(val value: String, val displayName: String) {
    QUESTION("question", "Soru"),
    EXPERIENCE("experience", "Deneyim"),
    PROBLEM("problem", "Sorun"),
    TIP("tip", "Tavsiye"),
    INFO("info", "Bilgilendirme"),
    DEBATE("debate", "Tartışma");

    companion object {
        fun fromValue(value: String): PostType =
            entries.find { it.value == value } ?: QUESTION
    }
}

// PostCategory ayrı dosyaya taşındı: PostCategory.kt
