package com.mulkora.core.common.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * ExifStripper — birim testleri.
 *
 * CLAUDE.md §3 (Faz 3 — Profil) gerekliliği:
 * "ExifStripper unit testi (konum temizlendi mi?)"
 *
 * Test senaryoları:
 * 1. GPS koordinatları temizleniyor mu?
 * 2. Cihaz modeli temizleniyor mu?
 * 3. Çıktı geçerli JPEG mi?
 * 4. Çıktı boş değil mi?
 * 5. Normal yön JPEG işleniyor mu?
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExifStripperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ── Yardımcı: GPS verisi gömülü test JPEG oluşturur ──────────────────────

    /**
     * GPS koordinatları ve cihaz bilgisi içeren küçük bir JPEG dosyası oluşturur.
     * ExifInterface ile meta veri eklenir, ardından URI döndürülür.
     */
    private fun createJpegWithExifGps(
        lat: Double = 41.0082,
        lng: Double = 28.9784,
        make: String = "TestMake",
        model: String = "TestModel",
    ): Uri {
        val tmpFile = File.createTempFile("exif_test_", ".jpg", context.cacheDir)

        // Küçük tek renkli bitmap yaz
        FileOutputStream(tmpFile).use { fos ->
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }

        // EXIF meta verilerini ekle
        ExifInterface(tmpFile.absolutePath).apply {
            // GPS koordinatları
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "41/1,0/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "28/1,58/1,42/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "50/1")
            setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0")
            setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, "10:00:00")
            setAttribute(ExifInterface.TAG_GPS_DATESTAMP, "2026:07:22")

            // Cihaz bilgisi
            setAttribute(ExifInterface.TAG_MAKE, make)
            setAttribute(ExifInterface.TAG_MODEL, model)
            setAttribute(ExifInterface.TAG_SOFTWARE, "TestSoftware 1.0")

            saveAttributes()
        }

        return Uri.fromFile(tmpFile)
    }

    // ── Test 1: GPS koordinatları temizlenmeli ───────────────────────────────

    /**
     * CLAUDE.md §16 kritik test:
     * Yüklenen fotoğrafın GPS koordinatları içermemesi zorunludur.
     */
    @Test
    fun `stripAndCompress removes GPS coordinates from output`() {
        val uri = createJpegWithExifGps(lat = 41.0082, lng = 28.9784)

        val cleanBytes = ExifStripper.stripAndCompress(context, uri)

        // Çıktıyı EXIF ile incele
        val exif = ExifInterface(ByteArrayInputStream(cleanBytes))

        val latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        val altitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)

        assertNull("GPS enlemi temizlenmemişti: $latitude", latitude)
        assertNull("GPS boylamı temizlenmemişti: $longitude", longitude)
        assertNull("GPS yüksekliği temizlenmemişti: $altitude", altitude)
    }

    // ── Test 2: GPS ref etiketleri de temizlenmeli ───────────────────────────

    @Test
    fun `stripAndCompress removes GPS reference tags`() {
        val uri = createJpegWithExifGps()

        val cleanBytes = ExifStripper.stripAndCompress(context, uri)
        val exif = ExifInterface(ByteArrayInputStream(cleanBytes))

        val latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
        val lngRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
        val timestamp = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP)

        assertNull("GPS enlem referansı temizlenmemişti", latRef)
        assertNull("GPS boylam referansı temizlenmemişti", lngRef)
        assertNull("GPS zaman damgası temizlenmemişti", timestamp)
    }

    // ── Test 3: Cihaz bilgisi temizlenmeli ───────────────────────────────────

    /**
     * Cihaz modeli ve üretici bilgisi çıktıda bulunmamalı.
     */
    @Test
    fun `stripAndCompress removes device make and model`() {
        val uri = createJpegWithExifGps(make = "SamsungTest", model = "Galaxy Test")

        val cleanBytes = ExifStripper.stripAndCompress(context, uri)
        val exif = ExifInterface(ByteArrayInputStream(cleanBytes))

        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

        assertNull("Cihaz üreticisi temizlenmemişti: $make", make)
        assertNull("Cihaz modeli temizlenmemişti: $model", model)
        assertNull("Yazılım versiyonu temizlenmemişti: $software", software)
    }

    // ── Test 4: Çıktı geçerli JPEG olmalı ────────────────────────────────────

    @Test
    fun `stripAndCompress returns valid JPEG bytes`() {
        val uri = createJpegWithExifGps()

        val cleanBytes = ExifStripper.stripAndCompress(context, uri)

        assertNotNull("Çıktı null olmamalı", cleanBytes)
        assertTrue("Çıktı boş olmamalı", cleanBytes.isNotEmpty())

        // JPEG sihirli baytları: 0xFF 0xD8 (SOI) ve 0xFF 0xD9 (EOI)
        val soiOk = cleanBytes[0] == 0xFF.toByte() && cleanBytes[1] == 0xD8.toByte()
        assertTrue("Çıktı geçerli JPEG başlığına sahip değil (SOI eksik)", soiOk)
    }

    // ── Test 5: Boyut sınırı korunmalı ───────────────────────────────────────

    @Test
    fun `stripAndCompress result fits within expected size bounds`() {
        val uri = createJpegWithExifGps()

        val cleanBytes = ExifStripper.stripAndCompress(context, uri)

        // 64×64 px JPEG %85 kalitede 50 KB'den büyük olamaz
        assertTrue(
            "Çıktı beklenmedik kadar büyük: ${cleanBytes.size} bytes",
            cleanBytes.size < 50 * 1024,
        )
        // Boş değil ve en az birkaç bayt içeriyor
        assertTrue("Çıktı çok küçük: ${cleanBytes.size} bytes", cleanBytes.size > 100)
    }

    // ── Test 6: Normal görsel (EXIF'siz) başarıyla işlenmeli ─────────────────

    @Test
    fun `stripAndCompress handles jpeg without any exif gracefully`() {
        // EXIF meta verisi olmayan saf bir JPEG oluştur
        val tmpFile = File.createTempFile("no_exif_", ".jpg", context.cacheDir)
        FileOutputStream(tmpFile).use { fos ->
            Bitmap.createBitmap(32, 32, Bitmap.Config.RGB_565)
                .compress(Bitmap.CompressFormat.JPEG, 80, fos)
        }
        val uri = Uri.fromFile(tmpFile)

        // Exception fırlatmamalı
        val cleanBytes = ExifStripper.stripAndCompress(context, uri)
        assertTrue("EXIF'siz görselden çıktı üretilemedi", cleanBytes.isNotEmpty())
    }
}