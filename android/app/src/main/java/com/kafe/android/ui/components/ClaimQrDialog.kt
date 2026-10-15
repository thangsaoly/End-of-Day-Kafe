package com.kafe.android.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.google.zxing.common.BitMatrix
import com.kafe.android.model.Reservation
import com.kafe.android.qr.encodeClaimQr

private val CoffeePrimary = Color(0xFF6F4E37)
private val DarkCoffee = Color(0xFF4E3629)

/** Full-screen-ish QR code the customer shows at the counter for the merchant to scan. */
@Composable
fun ClaimQrDialog(
    reservation: Reservation,
    onDismiss: () -> Unit
) {
    val qrImage = remember(reservation.claimCode) {
        encodeClaimQr(reservation.claimCode, sizePx = 512).toBitmap().asImageBitmap()
    }

    Dialog(onDismissRequest = onDismiss) {
        MaxScreenBrightness()

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = reservation.deal.cafe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkCoffee,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Show this code to the staff at pickup",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    bitmap = qrImage,
                    contentDescription = "QR code for claim code ${reservation.claimCode}",
                    // Nearest-neighbour scaling keeps the modules crisp for scanners
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = reservation.claimCode,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = DarkCoffee
                )
                Text(
                    text = "Pickup window: ${reservation.deal.pickupStart} - ${reservation.deal.pickupEnd}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close", color = CoffeePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Scanners read a bright screen far more reliably. The dialog has its own window,
// so brightness returns to normal automatically when the dialog closes.
@Composable
private fun MaxScreenBrightness() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    DisposableEffect(window) {
        window?.let { w ->
            val attrs = w.attributes
            attrs.screenBrightness = 1f
            w.attributes = attrs
        }
        onDispose { }
    }
}

private fun BitMatrix.toBitmap(): Bitmap {
    val pixels = IntArray(width * height) { i ->
        if (get(i % width, i / width)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
