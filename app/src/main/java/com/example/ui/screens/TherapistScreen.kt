package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessageEntity
import com.example.data.OrderEntity
import com.example.ui.PijatKuViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapistScreen(
    viewModel: PijatKuViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val activeOrder by viewModel.activeOrder.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val notifications by viewModel.therapistNotifications.collectAsState()

    var showDocSimulator by remember { mutableStateOf(false) }
    var showWithdrawForm by remember { mutableStateOf(false) }
    var withdrawAmountText by remember { mutableStateOf("") }
    var showChatView by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    val therapistOrders = allOrders.filter { it.therapistId == (user?.id ?: "") }
    val completedOrders = therapistOrders.filter { it.status == "SELESAI" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Header (Greeting, online toggle, wallet summary)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(NavyPrimary, NavySecondary)
                        ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = "Therapist Avatar",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Spesialis Terapis Mitra",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = user?.name ?: "Terapis",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Toggle switch for Online Active status
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (user?.isOnline == true) Color(0xFFD1FAE5) else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.setTherapistOnline(!(user?.isOnline ?: false)) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (user?.isOnline == true) MintGreen else Color.Gray,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (user?.isOnline == true) "ONLINE" else "OFFLINE",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = if (user?.isOnline == true) Color(0xFF065F46) else Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // E-Wallet Balance Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Pendapatan Dompet Mitra",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currencyFormatter.format(user?.balance ?: 0.0),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { showWithdrawForm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "WithDraw",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tarik Saldo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Statistics Grid (Daily, Monthly metrics)
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dashboard Kinerja Hari Ini",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val starsAvg = "%.1f".format(user?.rating ?: 4.8f).replace(",", ".")
                    MetricBadge(
                        label = "Order Selesai",
                        value = "${completedOrders.size} Order",
                        color = NavySecondary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Rating Kepala",
                        value = "$starsAvg / 5.0",
                        color = AccentGold,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Total Pelanggan",
                        value = "${user?.customerCount ?: 0} Orang",
                        color = MintGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Document Approval Status warning banner
        if (user?.status != "APPROVED") {
            item {
                val bannerColor = if (user?.status == "PENDING") Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                val textColor = if (user?.status == "PENDING") Color(0xFF92400E) else Color(0xFF991B1B)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerColor)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (user?.status == "PENDING") Icons.Default.HourglassEmpty else Icons.Default.Block,
                            contentDescription = "Status info",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (user?.status == "PENDING") "Pendaftaran Verifikasi Dokumen" else "Akun Bermasalah / Ditangguhkan",
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (user?.status == "PENDING") "Berkas Anda sedang diproses oleh admin. Silakan simulasi approve di panel Admin." else "Akun Anda di-suspend karena melanggar ketentuan. Hubungi support Mitra.",
                                color = textColor.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { showDocSimulator = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Berkas", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Live Incoming / Assigned active order!
        val assignedActiveOrder = activeOrder?.takeIf { it.therapistId == user?.id }
        if (assignedActiveOrder != null) {
            val order = assignedActiveOrder
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "KLIEN AKTIF: ${order.customerName}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFEECC))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = order.status,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Layanan: ${order.serviceName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavySecondary
                        )
                        Text(
                            text = "Alamat Penjemputan: ${order.address}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Gaji Anda (Setelah Potong Komisi): ${currencyFormatter.format(order.price * 0.8)}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MintGreen
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Interactive Canvas GPS Map Simulation
                        SimpleInteractiveMap(
                            customerLat = viewModel.customerLat.collectAsState().value,
                            customerLng = viewModel.customerLng.collectAsState().value,
                            therapistLat = viewModel.therapistLat.collectAsState().value,
                            therapistLng = viewModel.therapistLng.collectAsState().value,
                            therapistName = order.therapistName,
                            status = order.status
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions for Therapist
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.advanceOrderStatus() },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val actText = when (order.status) {
                                    "MENUNGGU" -> "Terima & Berangkat"
                                    "MENUJU_LOKASI" -> "Saya Sudah Tiba!"
                                    "TIBA" -> "Mulai Pemijatan"
                                    "MELAYANI" -> "Selesaikan Layanan"
                                    else -> "Proses"
                                }
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Setuju",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(actText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showChatView = true },
                                modifier = Modifier.weight(0.8f),
                                colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat Klien", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Historical Job logs list
        item {
            Text(
                text = "Riwayat Pekerjaan Selesai",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = NavyPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        if (completedOrders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Belum memiliki pekerjaan selesai. Naikkan status Online Anda untuk mulai menerima orderan.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(completedOrders) { order ->
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmeraldGreenLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                tint = MintGreen,
                                contentDescription = "Selesai"
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${order.customerName} • ${order.serviceName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Ganti Rugi Bersih: ${currencyFormatter.format(order.price * 0.8)} • Sukses",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            if (order.rating > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Ulasan Klien: ",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                    repeat(order.rating) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "star",
                                            tint = AccentGold,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    if (order.reviewComment.isNotEmpty()) {
                                        Text(
                                            text = " \"${order.reviewComment}\"",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Withdrawal Sliding Slip sheet Dialog
    if (showWithdrawForm) {
        AlertDialog(
            onDismissRequest = { showWithdrawForm = false },
            title = {
                Text(
                    text = "Tarik Dana Pendapatan 💳",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NavyPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Saldo dompet saat ini: ${currencyFormatter.format(user?.balance ?: 0.0)}",
                        fontWeight = FontWeight.SemiBold,
                        color = NavySecondary,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = withdrawAmountText,
                        onValueChange = { withdrawAmountText = it },
                        label = { Text("Jumlah Penarikan (Rp)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amt = withdrawAmountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && amt <= (user?.balance ?: 0.0)) {
                                viewModel.withdrawTherapistBalance(amt)
                                showWithdrawForm = false
                                withdrawAmountText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Konfirmasi Cairkan Rekening Bank", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWithdrawForm = false }) {
                    Text("Batal", color = Color.Gray, fontSize = 12.sp)
                }
            }
        )
    }

    // Document Simulation Upload Dialog
    if (showDocSimulator) {
        AlertDialog(
            onDismissRequest = { showDocSimulator = false },
            title = {
                Text(
                    text = "Kelayakan Mitra & Berkas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavyPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Simulasi dokumen Mitra Terunggah PijatKu:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    listOf(
                        "KTP Identitas Diri" to (user?.ktpDoc?.ifEmpty() { "KTP_Simulasi.jpg" } ?: "KTP_Simulasi.jpg"),
                        "Sertifikat Kesehatan / Kompetensi" to (user?.certDoc?.ifEmpty() { "Sertifikat_Simulasi.pdf" } ?: "Sertifikat_Simulasi.pdf"),
                        "Foto Selfie Diri" to (user?.selfieDoc?.ifEmpty() { "Foto_Selfie_Simulasi.jpg" } ?: "Foto_Selfie_Simulasi.jpg"),
                        "Foto Tempat Kerja / Homecare" to (user?.workplaceDoc?.ifEmpty() { "Foto_Workplace_Simulasi.jpg" } ?: "Foto_Workplace_Simulasi.jpg")
                    ).forEach { (docName, docFile) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = "Doc",
                                        tint = NavySecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(docName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(docFile, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    tint = MintGreen,
                                    contentDescription = "Uploaded",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocSimulator = false }) {
                    Text("Selesai & Keluar", color = NavyPrimary)
                }
            }
        )
    }

    // Modern Chat panel overlay dialog
    if (showChatView && activeOrder != null) {
        val messages by viewModel.currentChatMessages.collectAsState()
        val o = activeOrder!!

        AlertDialog(
            onDismissRequest = { showChatView = false },
            title = {
                Text(
                    text = "Obrolan dengan Klien 💬",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavyPrimary
                )
            },
            text = {
                Column {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .shadow(1.dp, RoundedCornerShape(8.dp))
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            if (messages.isEmpty()) {
                                Text(
                                    text = "Mulai obrolan untuk koordinasi lokasi penjemputan.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                messages.forEach { m ->
                                    val isMe = m.senderId == user?.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isMe) NavySecondary else Color(0xFFEFF6FF))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .widthIn(max = 180.dp)
                                        ) {
                                            Text(
                                                text = m.message,
                                                color = if (isMe) Color.White else Color.Black,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Ketik pesan ketik...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.sendChatMessage(chatInputText)
                                chatInputText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text("Kirim", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatView = false }) {
                    Text("Tutup", color = NavyPrimary)
                }
            }
        )
    }
}
