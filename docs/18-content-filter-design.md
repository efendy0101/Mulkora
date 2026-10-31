# Mülkora — İçerik Filtresi Tasarımı

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Amaç ve Kapsam

İçerik filtresi, kullanıcıların gönderdiği metin içeriklerin (gönderi başlığı, içerik, yorum) yayımlanmadan önce otomatik olarak incelenmesini sağlar. Filtre iki katmanda çalışır:

- **İstemci tarafı (Android):** Kullanıcıya anlık geri bildirim verme (UX amaçlı, güvenlik değil).
- **Sunucu tarafı (Cloud Functions):** Zorunlu kontrol; istemci sonucuna bakılmaksızın her gönderide çalışır.

---

## 2. Risk Seviyeleri

| Seviye | Kod | Açıklama | Sonuç |
|---|---|---|---|
| Temiz | `none` | Risk tespit edilmedi | Normal akış |
| Düşük Risk | `low` | Dikkat çeken ifade var | Kullanıcı uyarılır, devam edebilir |
| Orta Risk | `medium` | Potansiyel ihlal | Moderasyon kuyruğuna alınır |
| Yüksek Risk | `high` | Kural ihlali | Gönderi engellenir |
| Kritik | `critical` | Ciddi ihlal | Engellenir + yöneticiye bildirim |

---

## 3. Kontrol Kategorileri

### 3.1 Yasaklı Kelime Taraması

- Firestore `bannedTerms` koleksiyonundaki kelimeler ve regex desenleri ile karşılaştırma yapılır.
- Her yasaklı kelime bir risk seviyesi (`low` / `medium` / `high` / `critical`) ve kategori taşır.
- Kategoriler: `profanity` (küfür), `threat` (tehdit), `hate_speech` (nefret söylemi), `personal_info` (kişisel veri), `spam`.
- Bir içerikte birden fazla `low` risk kelimesi varsa birleşik risk seviyesi yükseltilir.

### 3.2 Kişisel Veri Tespiti

Aşağıdaki kişisel veriler regex ile tespit edilir:

| Veri Türü | Regex Deseni (örnek) | Risk Seviyesi |
|---|---|---|
| TC Kimlik No | `\b[1-9]\d{10}\b` | `high` |
| IBAN | `TR\d{24}` | `high` |
| Telefon Numarası | `\b(05\d{9})\b` | `medium` |
| E-posta Adresi | `\S+@\S+\.\S+` | `medium` |
| Kredi Kartı | `\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b` | `critical` |

### 3.3 Bağlantı ve Harici URL Tespiti

- İçerikte `http://`, `https://`, `www.` veya kısaltılmış URL bulunması → risk `low`.
- Belirli spam bilinen domainler → risk `medium` veya `high`.

### 3.4 Büyük Harf Oranı

- İçeriğin %70'inden fazlası büyük harf ise → risk `low` (spam işareti).

### 3.5 Tekrar Eden Karakter

- Aynı karakterin 5+ kez arka arkaya gelmesi (örn. "!!!!!!") → risk `low`.

---

## 4. Filtre Çalışma Akışı

```
Kullanıcı Gönderi/Yorum Gönderir
              │
              ▼
    Cloud Functions submitPost
              │
    ┌─────────▼──────────────┐
    │  İçerik Filtresi       │
    │                        │
    │  1. Yasaklı kelime     │
    │     taraması           │
    │                        │
    │  2. Kişisel veri       │
    │     tespiti            │
    │                        │
    │  3. URL tespiti        │
    │                        │
    │  4. Büyük harf         │
    │     oranı              │
    │                        │
    │  5. Risk birleştirme   │
    └────────────────────────┘
              │
    ┌─────────▼─────────────────────────────┐
    │                                       │
  none/low          medium             high/critical
    │                  │                    │
    ▼                  ▼                    ▼
  pending           filtered           Reddedildi
(mod. kuyruğu)   (mod. kuyruğu,      (kullanıcıya
                  yüksek öncelik)      hata mesajı)
                                       critical →
                                       admin bildirimi
```

---

## 5. Filtre Sonuç Nesnesi

```typescript
interface FilterResult {
  riskLevel: "none" | "low" | "medium" | "high" | "critical";
  triggeredRules: FilterRule[];
  details: {
    bannedTerms: string[];           // Tetiklenen yasaklı kelimeler
    personalDataTypes: string[];     // Tespit edilen kişisel veri türleri
    hasExternalLinks: boolean;
    capsRatio: number;               // 0.0 - 1.0
    repeatingCharsFound: boolean;
  };
}

interface FilterRule {
  category: string;
  riskLevel: string;
  matchedText: string;   // Log için, kullanıcıya gösterilmez
}
```

---

## 6. Cloud Functions İmplementasyonu

```typescript
// functions/src/util/contentFilter.ts

export async function filterContent(
  text: string,
  db: FirebaseFirestore.Firestore
): Promise<FilterResult> {

  // 1. Yasaklı kelime listesini Firestore'dan al (önbellekle)
  const bannedTerms = await getBannedTerms(db);

  const triggeredRules: FilterRule[] = [];
  let maxRisk: RiskLevel = "none";

  // 2. Yasaklı kelime taraması
  for (const term of bannedTerms) {
    if (!term.isActive) continue;
    
    const regex = term.isRegex
      ? new RegExp(term.term, 'gi')
      : new RegExp(`\\b${escapeRegex(term.term)}\\b`, 'gi');
    
    if (regex.test(text)) {
      triggeredRules.push({
        category: term.category,
        riskLevel: term.riskLevel,
        matchedText: '[REDACTED]'
      });
      maxRisk = elevateRisk(maxRisk, term.riskLevel);
    }
  }

  // 3. Kişisel veri tespiti
  const personalDataResult = detectPersonalData(text);
  if (personalDataResult.found) {
    triggeredRules.push(...personalDataResult.rules);
    maxRisk = elevateRisk(maxRisk, personalDataResult.maxRisk);
  }

  // 4. URL tespiti
  const hasExternalLinks = /https?:\/\/|www\./i.test(text);
  if (hasExternalLinks) {
    maxRisk = elevateRisk(maxRisk, "low");
  }

  // 5. Büyük harf oranı
  const letters = text.replace(/[^a-zA-ZğüşıöçĞÜŞİÖÇ]/g, '');
  const capsRatio = letters.length > 0
    ? letters.split('').filter(c => c === c.toUpperCase()).length / letters.length
    : 0;
  if (capsRatio > 0.7 && letters.length > 20) {
    maxRisk = elevateRisk(maxRisk, "low");
  }

  return {
    riskLevel: maxRisk,
    triggeredRules,
    details: {
      bannedTerms: triggeredRules.map(r => r.category),
      personalDataTypes: personalDataResult.types,
      hasExternalLinks,
      capsRatio,
      repeatingCharsFound: /(.)\1{4,}/.test(text)
    }
  };
}

function elevateRisk(current: RiskLevel, incoming: RiskLevel): RiskLevel {
  const order = ["none", "low", "medium", "high", "critical"];
  return order.indexOf(incoming) > order.indexOf(current) ? incoming : current;
}
```

---

## 7. Önbellek Stratejisi

`bannedTerms` koleksiyonu her Cloud Functions instance'ında bellek içi önbelleğe alınır.

```typescript
let cachedBannedTerms: BannedTerm[] | null = null;
let cacheTimestamp = 0;
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 dakika

async function getBannedTerms(db: Firestore): Promise<BannedTerm[]> {
  const now = Date.now();
  if (cachedBannedTerms && (now - cacheTimestamp) < CACHE_TTL_MS) {
    return cachedBannedTerms;
  }
  
  const snapshot = await db.collection('bannedTerms')
    .where('isActive', '==', true)
    .get();
  
  cachedBannedTerms = snapshot.docs.map(doc => doc.data() as BannedTerm);
  cacheTimestamp = now;
  return cachedBannedTerms;
}
```

---

## 8. İstemci Tarafı (Android) Filtresi

Android tarafında yalnızca temel filtre uygulanır; yasaklı kelimeler listesi uygulamaya gömülmez (güvenlik açığı yaratır ve liste güncel kalmaz).

**İstemci tarafı kontroller:**
- Boş içerik kontrolü
- Karakter sınırı kontrolü (başlık max 200, içerik max 5000)
- Temel URL varlığı uyarısı

---

## 9. EXIF Konum Verisi Temizleme

Görsel yükleme işleminde, Android uygulaması Storage'a yüklemeden önce EXIF verilerini siler.

```kotlin
// ExifStripper.kt
object ExifStripper {
    fun stripLocationData(imageFile: File): File {
        try {
            val exif = ExifInterface(imageFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
            exif.saveAttributes()
        } catch (e: Exception) {
            // EXIF temizleme başarısız olursa görsel yüklenmez
            throw SecurityException("EXIF temizleme başarısız oldu", e)
        }
        return imageFile
    }
}
```

**Not:** EXIF temizleme başarısız olursa görsel yükleme işlemi iptal edilir. Bu NFR-03.12 gereksinimini karşılar.

---

## 10. Filtre Metrikleri ve İzleme

Firebase Analytics ile aşağıdaki metrikler izlenir:

- `content_filter_triggered`: Filtre tetiklenme sayısı (risk seviyesiyle birlikte)
- `content_filter_risk_distribution`: Risk seviyesi dağılımı
- `post_rejected_by_filter`: Filtre nedeniyle reddedilen gönderi sayısı
- `filter_false_positive_rate`: Moderatörün filtreleri geçersiz sayma oranı

Bu metrikler `bannedTerms` listesinin etkinliğini değerlendirmek ve yanlış pozitif oranını düşürmek için kullanılır.

---

*Sonraki doküman: 19-notification-system.md*