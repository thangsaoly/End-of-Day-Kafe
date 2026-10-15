package com.kafe.android.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object ClaimCodes {
    // Backend format: "KAFE-" followed by uppercase letters/digits (currently 4)
    private val PATTERN = Regex("^KAFE-[A-Z0-9]{4,}$")

    /** Returns the cleaned-up claim code, or null if [raw] is not a Kafe claim code. */
    fun normalize(raw: String?): String? =
        raw?.trim()?.uppercase()?.takeIf { PATTERN.matches(it) }
}

/** Encodes a claim code as a square QR matrix of [sizePx] x [sizePx]. */
fun encodeClaimQr(claimCode: String, sizePx: Int): BitMatrix =
    QRCodeWriter().encode(
        claimCode,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
    )
