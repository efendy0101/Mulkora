# Mülkora

Mülkora, kiracı ve ev sahiplerinin deneyimlerini paylaşması için geliştirilmiş Android uygulaması ve yönetim panelidir. Bu depo Android istemcisini, Firebase Cloud Functions kodunu, Firestore/Storage kurallarını ve yönetim arayüzünü içerir.

## Bileşenler

- `app/`, `core/`, `feature/`: Kotlin ve Jetpack Compose ile Android uygulaması
- `admin-panel/`: Next.js yönetim paneli
- `functions/`: Firebase Cloud Functions (TypeScript)
- `firestore.rules`, `storage.rules`, `firestore.indexes.json`: Firebase kuralları ve indeksler
- `tests/`: güvenlik kuralları ve fonksiyon testleri
- `docs/`: ürün ve teknik tasarım notları

## Gereksinimler

- Node.js 22 (Cloud Functions), Node.js 24.12 veya üzeri (testler) ve npm; Firebase CLI (`npm install -g firebase-tools`)
- Firebase Emulator Suite için Java; Android derlemesi için Android Studio, Android SDK ve JDK 17
- Android uygulaması için kendi Firebase projenize ait `google-services.json`

## Yerel yönetim paneli demosu

Demo yalnızca `demo-mulkora` adlı yerel Firebase emülatör projesinde çalışır. İlk çalıştırmada emülatör bileşenleri indirilebilir. Aşağıdaki örnek komutlar PowerShell içindir; ayrı terminaller kullanın.

```powershell
cd functions
npm ci
npm run build
cd ..
firebase emulators:start --project demo-mulkora --only auth,firestore,functions
```

Emülatörler hazır olduğunda ikinci terminalde depo kökünden örnek verileri ekleyin:

```powershell
node scripts/seed-demo.cjs
```

Komut iki örnek kullanıcı, bir demo yönetici, kategoriler, iki yayımlanmış gönderi ve bir onay bekleyen gönderi oluşturur. Mevcut demo kayıtlarını silmez; demo yöneticisinin rolünü her çalıştırmada yeniden admin yapar; sıfırlamak için emülatörü yeni ve boş bir oturumla başlatın. Hesaplar ve içerikler tamamen örnektir.

Üçüncü terminalde:

```powershell
cd admin-panel
Copy-Item .env.demo.example .env.local
npm ci
npm run dev
```

Tarayıcıda http://localhost:3000 adresini açın. Demo yönetici: **admin@example.com** / **MulkoraDemo2026!**. Emülatör arayüzü http://localhost:4000 adresindedir. Bu parola yalnızca yerel demo içindir. `NEXT_PUBLIC_USE_EMULATOR=true` geliştirme sunucusunda emülatörlere bağlanır; üretim derlemesine veya gerçek Firebase projesine demo hesabı eklemez.

## Android kurulumu

Android Studio ile depo kökünü açın. Kendi Firebase projenizde `com.mulkora.app` paket adıyla Android uygulaması oluşturun ve indirdiğiniz yapılandırmayı `app/src/debug/google-services.json` konumuna koyun. Release için ayrı yapılandırma kullanın. Bu dosyalar, imzalama anahtarları ve `local.properties` Git'e dahil edilmez. Gradle eşitlemesinden sonra `app` yapılandırmasını çalıştırın.

Android istemcisi için yerel Firebase emülatörlerine otomatik bağlantı yapılandırması bu depoda bulunmaz; yukarıdaki demo akışı yönetim paneli içindir. Android'i kullanmadan önce Firebase ve ilgili servisleri kendi ortamınızda yapılandırın.

## Diğer yapılandırma

`admin-panel/.env.local.example` kendi Firebase projeniz için örnek değişkenleri içerir. `.firebaserc` yanlışlıkla gerçek projeye işlem yapılmasını önlemek için demo proje kimliği kullanır; dağıtım öncesinde doğru projeyi açıkça seçin. Canlı ortamda kimlik doğrulama, güvenlik kuralları, reklam ve ödeme entegrasyonları ayrıca yapılandırılmalı ve doğrulanmalıdır. Ayrıntılı kurulum notları: [KURULUM.md](KURULUM.md).

## Kontroller

```powershell
cd functions
npm run build
cd ../admin-panel
npm run build
```

Güvenlik kuralları testleri için `tests/security-rules/README.md` dosyasına bakın; testler Firebase emülatörlerini gerektirir.

## Gizli profil verileri için geçiş

`users/{uid}` belgeleri yalnızca sahibi ve moderatörler tarafından okunur. Diğer kullanıcıların görebileceği alanlar sunucu tarafından `publicProfiles/{uid}` içine kopyalanır; e-posta ve cihaz tokenları bu belgelerde yer almaz. Değişikliklerden önce var olan kullanıcılar için kurallar ve fonksiyonlar dağıtılmadan önce yedek alın, yeni `syncPublicProfile` fonksiyonunu dağıtın, ardından yetkili ortamda aşağıdaki aracı çalıştırın. Bu araç belirtilen projede genel profil belgelerini oluşturur veya yeniler; projeyi yanlış seçmeyin.

```powershell
$env:GCLOUD_PROJECT = "kendi-firebase-projeniz"
node scripts/backfill-public-profiles.cjs --confirm-project $env:GCLOUD_PROJECT
```

Geçişi test ortamında doğruladıktan sonra Firestore kurallarını ve yeni Android istemcisini yayımlayın. Eski istemciler diğer kullanıcıların özel `users` belgelerini okuyamaz. Yönetici ve moderatör yetkileri Firebase Auth custom claim üzerinden doğrulanır; mevcut yetkili kullanıcıların claim'lerini kontrol edin. Eski sürümde ayrıcalıklı rol yazılmış olabilecek kullanıcıları denetleyin. Kodun GitHub'a gönderilmesi, canlı Firebase kurallarını veya fonksiyonlarını değiştirmez.
