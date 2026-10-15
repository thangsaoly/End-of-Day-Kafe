package com.kafe.android.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClaimQrTest {

    @Test
    fun normalize_acceptsBackendFormat() {
        assertEquals("KAFE-1A2B", ClaimCodes.normalize("KAFE-1A2B"))
    }

    @Test
    fun normalize_trimsAndUppercases() {
        assertEquals("KAFE-1A2B", ClaimCodes.normalize("  kafe-1a2b\n"))
    }

    @Test
    fun normalize_rejectsOtherQrContent() {
        assertNull(ClaimCodes.normalize(null))
        assertNull(ClaimCodes.normalize(""))
        assertNull(ClaimCodes.normalize("https://example.com"))
        assertNull(ClaimCodes.normalize("KAFE-"))
        assertNull(ClaimCodes.normalize("KAFE-1A2B; DROP TABLE"))
    }

    @Test
    fun encodeClaimQr_roundTripsThroughDecoder() {
        val matrix = encodeClaimQr("KAFE-9F3C", sizePx = 256)
        assertEquals(256, matrix.width)
        assertEquals(256, matrix.height)

        val pixels = IntArray(matrix.width * matrix.height) { i ->
            if (matrix.get(i % matrix.width, i / matrix.width)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val source = RGBLuminanceSource(matrix.width, matrix.height, pixels)
        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)))

        assertEquals("KAFE-9F3C", decoded.text)
    }
}
