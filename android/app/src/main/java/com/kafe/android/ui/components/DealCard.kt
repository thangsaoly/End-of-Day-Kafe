package com.kafe.android.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kafe.android.model.SurplusDeal

// Palette inspired by warm coffee tones and premium emerald greens
private val CoffeePrimary = Color(0xFF6F4E37)
private val EmeraldGreen = Color(0xFF2E7D32)
private val LightRose = Color(0xFFFFEBEE)
private val BoldRed = Color(0xFFC62828)
private val SurfaceGrey = Color(0xFFF9F9F9)

@Composable
fun DealCard(
    deal: SurplusDeal,
    onReserveClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSoldOut = deal.quantityAvailable <= 0
    val cardBg = if (isSoldOut) Color(0xFFF0F0F0) else SurfaceGrey

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSoldOut) 1.dp else 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .border(
                width = 1.dp,
                color = if (isSoldOut) Color.LightGray else Color(0xFFE8E8E8),
                shape = RoundedCornerShape(16.dp)
            )
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Deal / Cafe Image
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = deal.imageUrl ?: deal.cafe.imageUrl,
                    contentDescription = "${deal.title} Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSoldOut) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SOLD OUT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info & Buttons
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = deal.cafe.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSoldOut) Color.Gray else CoffeePrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = deal.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSoldOut) Color.LightGray else Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Price Section
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format("%.2f", deal.discountPrice)}",
                        color = if (isSoldOut) Color.Gray else EmeraldGreen,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$${String.format("%.2f", deal.originalPrice)}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.LineThrough
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Pickup details
                Text(
                    text = "Pickup: ${deal.pickupStart} - ${deal.pickupEnd}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action / Quantity Section
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(90.dp)
            ) {
                // Stock Badge
                if (!isSoldOut) {
                    val badgeColor = if (deal.quantityAvailable <= 2) LightRose else Color(0xFFE8F5E9)
                    val textColor = if (deal.quantityAvailable <= 2) BoldRed else EmeraldGreen
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${deal.quantityAvailable} left",
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0E0E0))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "0 left",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Reserve Button
                Button(
                    onClick = { onReserveClick(deal.id) },
                    enabled = !isSoldOut,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeePrimary,
                        contentColor = Color.White,
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.DarkGray
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Reserve",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
