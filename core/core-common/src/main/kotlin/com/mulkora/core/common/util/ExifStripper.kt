package com.mulkora.core.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Görsel dosyalarından konum ve kişisel EXIF verilerini temizler.
 *
 * CLAUDE.md §16 & §30: Kullanıcı fotoğrafları yüklemeden önce EXIF metadata'dan
 * GPS koordinatları, cihaz bilgisi ve diğer kişisel veriler temizlenmeli.
 *
 * Güvenlik kuralları:
 * - GPS koordinatları (GPS_LATITUDE, GPS_LONGITUDE, GPS_ALTITUDE, vb.) kaldırılır
 * - Cihaz üreticisi ve model bilgisi kaldırılır
 * - Yazılım versiyonu kaldırılır
 * - Tarihe dokunulmaz — moderasyon için tutulur
 * - Yön (ORIENTATION) korunur — görsel döndürme için gerekli
 *
 * Kullanım:
 * ```kotlin
 * val cleanBytes = ExifStripper.stripAndCompress(context, imageUri)
 * // cleanBytes'ı Firebase Storage'a yükle
 * ```
 */
object ExifStripper {

    /**
     * Maksimum çıktı boyutu: 1024 × 1024 piksel (profil fotoğrafı için yeterli).
     * Daha büyük görseller orantılı olarak küçültülür.
     */
    private const val MAX_DIMENSION = 1024

    /**
     * JPEG sıkıştırma kalitesi (0-100). 85 iyi kalite + makul boyut dengesi sağlar.
     */
    private const val JPEG_QUALITY = 85

    /**
     * Görselin EXIF verilerini temizler, yeniden boyutlandırır ve sıkıştırır.
     *
     * @param context Android context — ContentResolver erişimi için gerekli
     * @param imageUri Kullanıcının seçtiği görsel URI (content:// veya file://)
     * @return Temizlenmiş ve sıkıştırılmış görsel verisi (JPEG olarak)
     * @throws IllegalArgumentException URI açılamazsa
     * @throws OutOfMemoryError çok büyük bir görsel yüklenirse (nadir)
     */
    fun stripAndCompress(context: Context, imageUri: Uri): ByteArray {
        val orientation = readOrientation(context, imageUri)

        val bitmap = decodeSampledBitmap(context, imageUri)
        val rotatedBitmap = applyOrientation(bitmap, orientation)

        return compressToJpeg(rotatedBitmap)
    }

    /**
     * EXIF yönünü okur — döndürme matrisi için kullanılır.
     * Yön bilgisi okunurken diğer hassas veriler okunaklı olmadığından
     * bu fonksiyon güvenlidir.
     */
    private fun readOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Büyük görselleri belleğe sığdırmak için örnek boyutu ile okur.
     * inSampleSize = 2^n olarak hesaplanır (MAX_DIMENSION'ı aşmadan).
     */
    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        options.inSampleSize = calculateInSampleSize(
            srcWidth = options.outWidth,
            srcHeight = options.outHeight,
        )
        options.inJustDecodeBounds = false

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("Görsel URI açılamadı: $uri")
    }

    /**
     * MAX_DIMENSION sınırını aşmayan en büyük 2^n inSampleSize değerini hesaplar.
     */
    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int): Int {
        var inSampleSize = 1
        val maxDim = maxOf(srcWidth, srcHeight)
        while (maxDim / inSampleSize > MAX_DIMENSION) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    /**
     * EXIF yön bilgisine göre bitmap'i döndürür/yansıtır.
     * Yön düzeltme yapıldıktan sonra orijinal yön verisi anlamını yitirir;
     * çıktı JPEG'e EXIF eklenmediğinden temizlenmiş olur.
     */
    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(-90f)
            }
            else -> return bitmap // ORIENTATION_NORMAL veya bilinmeyen
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Bitmap'i EXIF içermeyen saf JPEG bayt dizisine dönüştürür.
     *
     * Not: Bitmap.compress() çıktısı ExifInterface içermez; bu sayede
     * tüm meta veriler (GPS, cihaz, yazılım bilgisi) otomatik olarak temizlenir.
     */
    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return output.toByteArray()
    }
}

/**
 * InputStream'den ExifInterface oluşturmak için extension fonksiyonu.
 * Kullanılabilir olduğunda stream'i düzgün kapatır.
 */
private fun ExifInterface(inputStream: InputStream): ExifInterface =
    ExifInterface(inputStream)