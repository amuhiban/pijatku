package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.AccentGold

@Composable
fun SimpleInteractiveMap(
    customerLat: Double,
    customerLng: Double,
    therapistLat: Double,
    therapistLng: Double,
    therapistName: String,
    status: String,
    modifier: Modifier = Modifier
) {
    // Dynamic animated ripple wave for GPS indicators
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val waveRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_radius"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Land background
                drawRect(
                    color = Color(0xFFE2E8F0),
                    size = size
                )

                // Parks spaces
                drawRect(
                    color = Color(0xFFDCFCE7),
                    topLeft = Offset(width * 0.15f, height * 0.1f),
                    size = Size(width * 0.3f, height * 0.35f)
                )
                drawCircle(
                    color = Color(0xFFDCFCE7),
                    radius = width * 0.18f,
                    center = Offset(width * 0.8f, height * 0.75f)
                )

                // Blue waterway/river
                val riverPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height * 0.8f)
                    cubicTo(
                        width * 0.3f, height * 0.75f,
                        width * 0.6f, height * 0.95f,
                        width, height * 0.88f
                    )
                }
                drawPath(
                    path = riverPath,
                    color = Color(0xFFBFDBFE),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f)
                )

                // Grid lines representing town streets
                val streetColor = Color(0xFFF1F5F9)
                val strokeWidth = 8f

                // Horizontal streets
                drawLine(streetColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth)
                drawLine(streetColor, Offset(0f, height * 0.55f), Offset(width, height * 0.55f), strokeWidth)
                drawLine(streetColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth)

                // Vertical/diagonal streets
                drawLine(streetColor, Offset(width * 0.25f, 0f), Offset(width * 0.25f, height), strokeWidth)
                drawLine(streetColor, Offset(width * 0.55f, 0f), Offset(width * 0.45f, height), strokeWidth)
                drawLine(streetColor, Offset(width * 0.8f, 0f), Offset(width * 0.85f, height), strokeWidth)

                // Customer X, Y center mapping
                val custX = width * 0.65f
                val custY = height * 0.45f

                // Cast difference to Float precisely to prevent type mismatch
                val latDiff = ((therapistLat - customerLat) * 30000.0).toFloat()
                val lngDiff = ((therapistLng - customerLng) * 30000.0).toFloat()

                val therX = (custX + lngDiff).coerceIn(40f, width - 40f)
                val therY = (custY - latDiff).coerceIn(40f, height - 40f)

                // Draw connecting route line if tracking
                if (status != "MENUNGGU" && status != "SELESAI" && status != "BATAL") {
                    drawLine(
                        color = NavySecondary,
                        start = Offset(therX, therY),
                        end = Offset(custX, custY),
                        strokeWidth = 6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                }

                // --- Draw Customer Marker (Blue) ---
                drawCircle(
                    color = NavySecondary.copy(alpha = waveAlpha),
                    radius = waveRadius + 8f,
                    center = Offset(custX, custY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = Offset(custX, custY)
                )
                drawCircle(
                    color = NavySecondary,
                    radius = 8f,
                    center = Offset(custX, custY)
                )

                // --- Draw Therapist Marker (Green) ---
                if (status != "SELESAI" && status != "BATAL") {
                    drawCircle(
                        color = MintGreen.copy(alpha = waveAlpha),
                        radius = waveRadius,
                        center = Offset(therX, therY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 14f,
                        center = Offset(therX, therY)
                    )
                    drawCircle(
                        color = MintGreen,
                        radius = 10f,
                        center = Offset(therX, therY)
                    )
                }
            }

            // Status Badge Overlay
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (status == "MENUNGGU" || status == "SELESAI") Color.Gray else MintGreen,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (status == "MENUNGGU") "Mencari Terapis..." else "Live GPS tracking",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bottom Label Overlay
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Text(
                    text = if (status == "MENUNGGU" || status == "SELESAI") "Radius Pelayanan: 5 Km" else "Terapis: $therapistName",
                    color = NavyPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun InteractiveStarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "$i Bintang",
                tint = if (i <= rating) AccentGold else Color.Gray,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onRatingChange(i) }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun MetricBadge(
    label: String,
    value: String,
    color: Color = NavySecondary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
