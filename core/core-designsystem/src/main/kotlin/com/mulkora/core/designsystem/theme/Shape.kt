package com.mulkora.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Mülkora şekil sistemi — Material 3 Shapes.
 * Kart ve bileşenlerde tutarlı köşe yarıçapı sağlar.
 */
val MulkoraShapes = Shapes(
    // Küçük bileşenler: chip, badge, küçük buton
    extraSmall = RoundedCornerShape(4.dp),

    // Küçük bileşenler: text field, küçük kart
    small = RoundedCornerShape(8.dp),

    // Orta bileşenler: kart, diyalog
    medium = RoundedCornerShape(12.dp),

    // Büyük bileşenler: bottom sheet, geniş kart
    large = RoundedCornerShape(16.dp),

    // Çok büyük bileşenler: tam ekran bottom sheet
    extraLarge = RoundedCornerShape(24.dp)
)