package com.mulkora.feature.billing.domain.model

/**
 * Kullanıcının reklam kaldırma satın alma durumu.
 *
 * CLAUDE.md §18: Satın alma durumu yalnızca yerel depolamada tutulmamalıdır.
 * Google Play satın alma tokenı backend üzerinden doğrulanmalıdır.
 * Uygulama yeniden kurulsa bile satın alma geri yüklenir.
 */
enum class PurchaseStatus {
    /** Durum henüz kontrol edilmedi. */
    UNKNOWN,

    /** Kullanıcı satın alma yapmamış. */
    NOT_PURCHASED,

    /** Satın alma işlemi başlatıldı, backend doğrulaması bekleniyor. */
    PENDING,

    /** Backend tarafından doğrulandı, reklamlar kaldırıldı. */
    PURCHASED,

    /** Satın alma iade edildi; reklamlar yeniden gösterilmeli. */
    REFUNDED,
}

/**
 * Satın alma ürün tanımlayıcısı.
 * CLAUDE.md §18: Önerilen ürün kimliği `remove_ads_lifetime`.
 */
object BillingProductIds {
    const val REMOVE_ADS_LIFETIME = "remove_ads_lifetime"
}