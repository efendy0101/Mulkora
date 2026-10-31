# Mülkora — Fonksiyonel Olmayan Gereksinimler

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## NFR-01 Performans

- NFR-01.1: Ana akış (feed) ilk yüklemesi 2 saniyenin altında tamamlanmalıdır (orta hızlı mobil ağ koşullarında, 4G).
- NFR-01.2: Sayfa geçişleri 300 ms'nin altında olmalıdır.
- NFR-01.3: Gönderi detay sayfası 1,5 saniyenin altında yüklenmelidir.
- NFR-01.4: Görsel yükleme başarı oranı %99'un üzerinde olmalıdır.
- NFR-01.5: Paging 3 ile sayfalama, akışta gözle görülür yavaşlama olmaksızın çalışmalıdır.
- NFR-01.6: Firebase Firestore sorgularında gereksiz okuma sayısı minimumda tutulmalıdır; pagination ve kısmi yükleme uygulanmalıdır.
- NFR-01.7: Görseller Coil ile önbelleğe alınmalı; disk ve bellek önbellek stratejisi uygulanmalıdır.
- NFR-01.8: Cloud Functions soğuk başlangıç (cold start) süresi kritik işlemler için minimum tutulmalıdır.

---

## NFR-02 Güvenilirlik ve Erişilebilirlik

- NFR-02.1: Uygulama çökme oranı Firebase Crashlytics ile izlenmeli; aylık çökme oranı %0,5'in altında tutulmalıdır.
- NFR-02.2: Firebase Firestore çevrimdışı kalıcılığı (offline persistence) aktif olmalıdır; internet bağlantısı kesildiğinde uygulama kullanılabilir kalmalıdır.
- NFR-02.3: İnternet bağlantısı kesildiğinde kullanıcıya açık hata mesajı gösterilmelidir.
- NFR-02.4: Uygulama arka plana alınıp tekrar açıldığında oturum ve akış durumu korunmalıdır.
- NFR-02.5: WorkManager ile yürütülen arka plan görevleri, cihaz yeniden başlatılsa bile devam etmelidir.
- NFR-02.6: API veya Firebase hatalarında kullanıcıya anlamlı hata mesajları gösterilmelidir; stack trace veya teknik detay gösterilmemelidir.

---

## NFR-03 Güvenlik

- NFR-03.1: Firebase App Check tüm Cloud Functions ve Firestore okuma/yazma işlemlerinde zorunlu olmalıdır.
- NFR-03.2: Tüm gelen veriler hem istemci (Android, Web Panel) hem de sunucu (Cloud Functions) tarafında doğrulanmalıdır.
- NFR-03.3: Firestore Security Rules, Emulator Suite ile kapsamlı şekilde test edilmelidir.
- NFR-03.4: Storage Security Rules dosya sahipliğini ve içerik türünü doğrulamalıdır.
- NFR-03.5: Tüm API anahtarları, Firebase yapılandırması ve hassas bilgiler kaynak koduna yazılmamalıdır; güvenli yapılandırma dosyaları veya ortam değişkenleri kullanılmalıdır.
- NFR-03.6: JWT token'ları ve Firebase custom claims kullanıcı oturumunun sunucu tarafında doğrulanması için kullanılmalıdır.
- NFR-03.7: Kullanıcı adı, profil fotoğrafı ve rol bilgileri dışında kullanıcının kişisel bilgileri diğer kullanıcılara gösterilmemelidir.
- NFR-03.8: Cloud Functions'da rate limiting uygulanmalıdır; aynı kullanıcıdan kısa sürede çok sayıda istek gelmesi engellenmelidir.
- NFR-03.9: Beğeni, eksi oy ve puan sayaçları istemciden doğrudan Firestore'a yazılamamalıdır; Cloud Functions üzerinden işlenmelidir.
- NFR-03.10: Satın alma tokenı istemcide saklanmaz; yalnızca backend doğrulaması geçerlidir.
- NFR-03.11: MIME türü, dosya uzantısı ve dosya boyutu kontrolleri görsel yüklemede zorunludur.
- NFR-03.12: Görsel dosyalarından EXIF konum verisi yükleme öncesinde silinmelidir.
- NFR-03.13: Debug sürümünde test Firebase projesi ve test AdMob kimlikleri kullanılmalıdır; production anahtarları debug build'e dahil edilmemelidir.

---

## NFR-04 Sürdürülebilirlik ve Kod Kalitesi

- NFR-04.1: Uygulama Clean Architecture prensiplerine (data / domain / presentation katmanları) uygun geliştirilmelidir.
- NFR-04.2: MVVM deseni tüm feature modüllerinde uygulanmalıdır.
- NFR-04.3: Hilt ile bağımlılık enjeksiyonu kullanılmalıdır; manual dependency wiring yapılmamalıdır.
- NFR-04.4: Coroutines ve StateFlow ile reaktif programlama uygulanmalıdır; LiveData tercih edilmemelidir.
- NFR-04.5: Compose ekranları doğrudan Firestore'a erişmemelidir; tüm veri işlemleri Repository katmanından geçmelidir.
- NFR-04.6: Her feature modülü kendi data source, DTO/entity, mapper, repository, use case, ViewModel, UI state ve ekranını içermelidir.
- NFR-04.7: Production kodunda TODO ve FIXME bırakılmamalıdır.
- NFR-04.8: Sahte (stub/mock) servis veya implementasyon production kodunda bulunmamalıdır.
- NFR-04.9: Tüm Kotlin dosyaları derleme hatası vermemelidir.
- NFR-04.10: Import eksikliği bırakılmamalıdır.
- NFR-04.11: Veri modeli değişikliklerinde migration planı oluşturulmalıdır.

---

## NFR-05 Test Edilebilirlik

- NFR-05.1: Her use case için birim test (unit test) yazılmalıdır.
- NFR-05.2: Her ViewModel için StateFlow tabanlı birim test yazılmalıdır.
- NFR-05.3: Compose ekranları için UI test yazılmalıdır.
- NFR-05.4: Cloud Functions için Jest tabanlı test yazılmalıdır.
- NFR-05.5: Firestore Security Rules için Firebase Emulator Suite tabanlı test yazılmalıdır.
- NFR-05.6: Satın alma akışı test ortamında doğrulanmalıdır.
- NFR-05.7: Referans kötüye kullanım senaryoları test edilmelidir.
- NFR-05.8: Sözleşme erişim yetki testleri yazılmalıdır.

---

## NFR-06 Erişilebilirlik (Accessibility)

- NFR-06.1: TalkBack desteği sağlanmalıdır; tüm etkileşimli öğelerde contentDescription bulunmalıdır.
- NFR-06.2: Dinamik yazı boyutu (Dynamic Type) desteklenmelidir; sabit piksel boyutları kullanılmamalıdır.
- NFR-06.3: WCAG AA renk kontrast oranı standartlarına uyulmalıdır.
- NFR-06.4: Dokunma hedef alanları minimum 48dp × 48dp olmalıdır.
- NFR-06.5: Açık ve koyu tema desteklenmelidir.
- NFR-06.6: Renk bilgisi tek başına anlam taşımamalıdır; ikon veya metin ile desteklenmelidir.

---

## NFR-07 Ölçeklenebilirlik

- NFR-07.1: Firestore veri modeli, koleksiyon büyüdükçe sorgular yavaşlamayacak şekilde tasarlanmalıdır; bileşik indeksler tanımlanmalıdır.
- NFR-07.2: Cloud Functions yatay ölçeklemeye uygun olmalıdır; stateful işlemler yapılmamalıdır.
- NFR-07.3: Firebase Remote Config kullanılarak özellik bayrakları (feature flags) yönetilebilmelidir.
- NFR-07.4: AdMob yapılandırması ve reklam sıklık kuralları Remote Config üzerinden güncellenebilmelidir.

---

## NFR-08 KVKK ve Gizlilik Uyumu

- NFR-08.1: Yalnızca gerekli veriler toplanmalıdır; fazladan veri talep edilmemelidir.
- NFR-08.2: Kullanıcıya toplanan veriler ve kullanım amacı açıkça bildirilmelidir.
- NFR-08.3: Açık rıza ile zorunlu veri işleme süreçleri ayrılmalıdır.
- NFR-08.4: Kullanıcı verilerini indirme talebi akışı uygulanmalıdır.
- NFR-08.5: Hesap silme ve veri anonimleştirme akışı uygulanmalıdır.
- NFR-08.6: Hassas veriler (adres, sözleşme bilgileri) genel kullanıcılara gösterilmemelidir.
- NFR-08.7: Bildirim içeriklerinde hassas kişisel veri yer almamalıdır.
- NFR-08.8: Cihaz izin talepleri gereğinden fazla olmamalıdır; kullanılan izinler için kullanıcıya açıklama yapılmalıdır.
- NFR-08.9: Google Play Veri Güvenliği formu doğru şekilde doldurulmalıdır.

---

## NFR-09 Kullanılabilirlik (Usability)

- NFR-09.1: Tasarım dili modern, güven veren, tarafsız, şık, sade ve kullanıcı dostudur.
- NFR-09.2: Material 3 tasarım sistemine uyulmalıdır; tutarsız bileşen kullanımı bulunmamalıdır.
- NFR-09.3: Kiracı ve ev sahibi platformda eşit biçimde temsil edilmelidir; tasarım hiçbir tarafı kayırmaz.
- NFR-09.4: Kullanıcı bir işlem yaparken bekleme durumunda ilerleme göstergesi (loading indicator) gösterilmelidir.
- NFR-09.5: Form doğrulama hataları satır içinde (inline) ve Türkçe olarak gösterilmelidir.
- NFR-09.6: Geri alma (undo) desteği: Yorum silme gibi kritik işlemlerde SnackBar ile geri alma seçeneği sunulmalıdır.

---

## NFR-10 Yerelleştirme (Localization)

- NFR-10.1: Uygulama birincil dili Türkçedir.
- NFR-10.2: Dil altyapısı ikinci dil (örn. İngilizce) eklenmeye hazır olmalıdır; string'ler hardcode yazılmamalıdır.
- NFR-10.3: Tarih ve saat gösterimi Türkiye yerel ayarlarına (tr-TR) göre yapılmalıdır.
- NFR-10.4: Para birimi gösterimi Türk Lirası (₺) formatında olmalıdır.

---

## NFR-11 CI/CD ve Geliştirme Süreci

- NFR-11.1: Debug ve Release build varyantları ayrı Firebase projelerine bağlanmalıdır.
- NFR-11.2: Gradle build dosyaları Kotlin DSL (build.gradle.kts) ile yazılmalıdır.
- NFR-11.3: ProGuard/R8 kuralları kritik sınıflar için yapılandırılmalıdır.
- NFR-11.4: Uygulama imzalama bilgileri kaynak koduna yazılmamalıdır; CI ortam değişkenlerinden alınmalıdır.
- NFR-11.5: Firebase Emulator Suite geliştirme ortamında kullanılmalıdır; production Firebase'e erişilmemelidir.

---

*Sonraki doküman: 04-user-roles.md*