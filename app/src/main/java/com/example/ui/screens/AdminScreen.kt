package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConfigEntity
import com.example.data.VoucherEntity
import com.example.ui.PijatKuViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: PijatKuViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val config by viewModel.adminConfig.collectAsState()
    val vouchers by viewModel.allVouchers.collectAsState()

    var activeTab by remember { mutableStateOf("METRIK") } // "METRIK", "APPROVE", "ROUTING", "PROMO"

    // Form states for config
    var commissionInput by remember { mutableStateOf("%.1f".format(config.platformCommissionPercent)) }
    var serviceFeeInput by remember { mutableStateOf(config.serviceFee.toInt().toString()) }

    // Form states for adding coupon
    var newCodeInput by remember { mutableStateOf("") }
    var newDescInput by remember { mutableStateOf("") }
    var newDiscountInput by remember { mutableStateOf("") }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    val customerCount = users.count { it.role == "CUSTOMER" }
    val therapistCount = users.count { it.role == "THERAPIST" }

    val totalRevenue = orders.filter { it.status == "SELESAI" }.sumOf { it.price }
    val commissionVolume = totalRevenue * (config.platformCommissionPercent / 100.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Admin Control Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Panel Admin Utama PijatKu",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Dashboard Konsol Bisnis",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Settings,
                            tint = MintGreen,
                            contentDescription = "Admin Setting",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Transaksi Selesai",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = currencyFormatter.format(totalRevenue),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Laba Bersih Aplikasi (${config.platformCommissionPercent.toInt()}%)",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = currencyFormatter.format(commissionVolume),
                                color = MintGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Metrics Statistics Card List
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBadge(
                    label = "Pelanggan",
                    value = "$customerCount Orang",
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricBadge(
                    label = "Mitra Terapis",
                    value = "$therapistCount Mitra",
                    color = NavySecondary,
                    modifier = Modifier.weight(1f)
                )
                MetricBadge(
                    label = "Total Transaksi",
                    value = "${orders.size} Pesanan",
                    color = MintGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Selection Tabs buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "METRIK" to "Lap & Komisi",
                    "APPROVE" to "Berkas Terapis",
                    "PROMO" to "Kupon Diskon"
                ).forEach { (tabId, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == tabId) NavySecondary else Color(0xFFE2E8F0))
                            .clickable { activeTab = tabId }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (activeTab == tabId) Color.White else Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tab Content switcher
        when (activeTab) {
            "METRIK" -> {
                // Commission configurations & native canvas data plotting report
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    tint = NavySecondary,
                                    contentDescription = "Exchange"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Konfigurasi Komisi & Tarif Platform",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = commissionInput,
                                onValueChange = { commissionInput = it },
                                label = { Text("Komisi Aplikasi PijatKu (%)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = serviceFeeInput,
                                onValueChange = { serviceFeeInput = it },
                                label = { Text("Tarif Layanan Dasar Aplikasi (Rp)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val comm = commissionInput.replace(",", ".").toFloatOrNull() ?: 20.0f
                                    val fee = serviceFeeInput.toDoubleOrNull() ?: 5000.0
                                    viewModel.updatePlatformConfig(comm, fee)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Simpan Konfigurasi Baru", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Native report chart drawing
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Laporan Pendapatan Mingguan Mitra",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NavyPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bagan laporan pertumbuhan omzet harian PijatKu harian:",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Canvas Bar Grid
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                val w = size.width
                                val h = size.height

                                // Draw baseline
                                drawLine(
                                    color = Color.LightGray,
                                    start = Offset(40f, h - 30f),
                                    end = Offset(w - 20f, h - 30f),
                                    strokeWidth = 3f
                                )

                                val weeklyData = listOf(350000f, 480000f, 290000f, 620000f, 850000f, 920000f, 1050000f)
                                val days = listOf("Sen", "Sel", "Rab", "Kam", "jum", "Sab", "min")
                                val maxVal = weeklyData.maxOrNull() ?: 1000000f

                                val barSpacing = (w - 70f) / 7
                                val barWidth = 30f

                                for (i in weeklyData.indices) {
                                    val scaleAmt = weeklyData[i] / maxVal
                                    val barHeight = (h - 50f) * scaleAmt
                                    val xOffset = 50f + (i * barSpacing)
                                    val yOffset = h - 30f - barHeight

                                    drawRect(
                                        color = if (i == 6) MintGreen else NavySecondary,
                                        topLeft = Offset(xOffset, yOffset),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Ming").forEach { day ->
                                    Text(
                                        text = day,
                                        fontSize = 11.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.width(30.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "APPROVE" -> {
                // Interactive therapist approval lists. Lets you approve Eko Prasetyo
                item {
                    Text(
                        text = "Kelayakan Verifikasi Dokumen & Suspend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                val therapistUsers = users.filter { it.role == "THERAPIST" }

                if (therapistUsers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "Tidak ada pendaftar terapis saat ini.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(therapistUsers) { therapist ->
                        val isPending = therapist.status == "PENDING"
                        val isSuspended = therapist.status == "SUSPENDED"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .shadow(1.dp, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = therapist.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "Email: ${therapist.email} • Telp: ${therapist.phone}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (therapist.status) {
                                                    "APPROVED" -> Color(0xFFD1FAE5)
                                                    "PENDING" -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFFEE2E2)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = therapist.status,
                                            color = when (therapist.status) {
                                                "APPROVED" -> Color(0xFF065F46)
                                                "PENDING" -> Color(0xFF92400E)
                                                else -> Color(0xFF991B1B)
                                            },
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Dokumen Verifikasi: KTP (${therapist.ktpDoc.ifEmpty() { "LENGKAP" }}), Sertifikat (${therapist.certDoc.ifEmpty() { "LENGKAP" }})",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isPending) {
                                        Button(
                                            onClick = { viewModel.updateTherapistApproval(therapist.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Setujui Berkas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.suspendUser(therapist.id, !isSuspended) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSuspended) MintGreen else Color.Red
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (isSuspended) "Buka Suspend" else "Suspend Akun",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "PROMO" -> {
                // Interactive coupon dynamic additions form & listings
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Buat Kupon Voucher Baru 🎫",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NavyPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = newCodeInput,
                                onValueChange = { newCodeInput = it },
                                label = { Text("Kode Kupon (Contoh: MINTRELAX)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newDiscountInput,
                                    onValueChange = { newDiscountInput = it },
                                    label = { Text("Potongan Harga (Rp)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = newDescInput,
                                onValueChange = { newDescInput = it },
                                label = { Text("Keterangan singkat Promo", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val amt = newDiscountInput.toDoubleOrNull() ?: 10000.0
                                    val code = newCodeInput.trim()
                                    if (code.isNotEmpty()) {
                                        viewModel.addVoucher(
                                            code = code,
                                            discount = amt,
                                            desc = newDescInput,
                                            isCashback = false
                                        )
                                        // Reset fields
                                        newCodeInput = ""
                                        newDescInput = ""
                                        newDiscountInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Buat & Daftarkan Voucher", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Daftar Kupon Aktif di PijatKu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(vouchers) { voucher ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .background(NavySecondary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = voucher.code,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = voucher.description,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Efek: Potongan Rp ${voucher.discountAmount.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintGreen
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteVoucher(voucher.code) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    tint = Color.Red,
                                    contentDescription = "Hapus"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
