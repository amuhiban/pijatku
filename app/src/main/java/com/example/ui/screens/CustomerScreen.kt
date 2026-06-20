package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint
import com.example.R
import com.example.data.OrderEntity
import com.example.ui.PijatKuViewModel
import com.example.ui.theme.MintGreen
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.AccentGold
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.TextDark
import java.text.NumberFormat
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: PijatKuViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val activeOrder by viewModel.activeOrder.collectAsState()
    val rawTherapists by viewModel.allTherapists.collectAsState()
    val therapists = rawTherapists.filter { it.status == "APPROVED" }
    val appliedVoucher by viewModel.appliedVoucher.collectAsState()
    val notifications by viewModel.customerNotifications.collectAsState()
    val allOrdersVal by viewModel.allOrders.collectAsState()

    var showNotifications by remember { mutableStateOf(false) }
    var promoInputText by remember { mutableStateOf("") }
    var showBookingForm by remember { mutableStateOf(false) }
    var showChatView by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    var showCancelConfirmation by remember { mutableStateOf(false) }

    var showMapMode by remember { mutableStateOf(false) }
    var selectedMapTherapist by remember { mutableStateOf<com.example.data.UserEntity?>(null) }

    // Forms for Reviewing
    var ratingInput by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    var useDemoMode by remember { mutableStateOf(false) }
    val isLoggedInWithFirebase = user != null && user?.id != "cust_ahmad"

    if (!isLoggedInWithFirebase && !useDemoMode) {
        FirebaseAuthScreen(
            viewModel = viewModel,
            onDemoModeClick = {
                useDemoMode = true
                viewModel.loginAs("cust_ahmad")
            }
        )
        return
    }

    // Safe top-level extraction of recently completed unrated order
    val completedUnratedOrder = allOrdersVal.firstOrNull { 
        it.customerId == (user?.id ?: "") && it.status == "SELESAI" && it.rating == 0 
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedMassageType by remember { mutableStateOf("Semua") }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = {
                Text(
                    text = "Batalkan Pesanan?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = NavyPrimary
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin membatalkan pesanan layanan pijat ini? Tindakan ini tidak dapat diurungkan.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOrder()
                        showCancelConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Ya, Batalkan", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text("Kembali", color = Color.Gray, fontSize = 12.sp)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
    var minRating by remember { mutableStateOf(0.0) }
    var showOnlyActive by remember { mutableStateOf(false) }

    fun getTherapistSpecialty(name: String): String {
        return when {
            name.contains("Budi", ignoreCase = true) -> "Pijat Tradisional Jawa, Urut Capek, Refleksi Saraf Kaki"
            name.contains("Ani", ignoreCase = true) -> "Pijat Ibu & Anak, Totok Aura Wajah, Pijat Relaksasi Swedia"
            name.contains("Joko", ignoreCase = true) -> "Pijat Aromaterapi Kesehatan, Bekam Kering, Deep Tissue Therapy"
            else -> "Terapi Kebal Pegal, Pijat Kebugaran, Kop Masuk Angin"
        }
    }

    val filteredTherapists = therapists.filter { therapist ->
        val specialty = getTherapistSpecialty(therapist.name)
        val matchesActive = !showOnlyActive || therapist.isOnline
        val matchesSearch = searchQuery.isBlank() || 
                therapist.name.contains(searchQuery, ignoreCase = true) ||
                specialty.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedMassageType == "Semua" || when (selectedMassageType) {
            "Tradisional" -> specialty.contains("Tradisional", ignoreCase = true) || specialty.contains("Urut", ignoreCase = true)
            "Refleksi" -> specialty.contains("Refleksi", ignoreCase = true)
            "Relaksasi" -> specialty.contains("Relaksasi", ignoreCase = true) || specialty.contains("Aromaterapi", ignoreCase = true)
            "Ibu & Anak" -> specialty.contains("Ibu", ignoreCase = true) || specialty.contains("Anak", ignoreCase = true)
            else -> true
        }
        val matchesRating = therapist.rating >= minRating

        matchesActive && matchesSearch && matchesType && matchesRating
    }

    var activeTab by remember { mutableStateOf("home") } // "home" or "history"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        if (activeTab == "home") {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
        // 1. Header (User profile, greeting, wallet)
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
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User Avatar",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Halo, Selamat Datang!",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = user?.name ?: "Customer",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Notification Icon with badge
                            Box {
                                IconButton(onClick = { showNotifications = !showNotifications }) {
                                    Icon(
                                        imageVector = if (notifications.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        tint = if (notifications.isNotEmpty()) MintGreen else Color.White,
                                        contentDescription = "Notifikasi"
                                    )
                                }
                                if (notifications.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color.Red, CircleShape)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-4).dp, y = 4.dp)
                                    )
                                }
                            }

                            if (isLoggedInWithFirebase) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        useDemoMode = false
                                        viewModel.logout()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        tint = Color.White,
                                        contentDescription = "Keluar Firebase"
                                    )
                                }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Wallet",
                                        tint = MintGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Saldo PijatPay",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currencyFormatter.format(user?.balance ?: 0.0),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row {
                                Button(
                                    onClick = {
                                        viewModel.topUpCustomerBalance(100000.0)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "Isi Saldo",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Top Up", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1.5 Pick up location address switcher card mimicking the Tailwind layout
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(1.dp, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📍", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LOKASI PENJEMPUTAN",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            val customAddr = viewModel.customAddress.collectAsState().value
                            Text(
                                text = if (customAddr.isNotEmpty()) customAddr else "Apartemen Sudirman Park, Jakarta",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = "UBAH",
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable {
                                viewModel.customAddress.value = "Kuningan City Mall, Jakarta Selatan"
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Dropdown Notifications
        if (showNotifications) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pemberitahuan Realtime",
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tutup",
                                color = NavySecondary,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { showNotifications = false }
                                    .padding(4.dp)
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        if (notifications.isEmpty()) {
                            Text(
                                text = "Tidak ada notifikasi baru.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            notifications.forEach { notif ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = "Alert",
                                        tint = MintGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = notif.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = notif.description,
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                                Divider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }

        // 2. Active Order Status Tracker (Real-time updates)
        if (activeOrder != null) {
            val order = activeOrder!!
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
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = "Running",
                                    tint = MintGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Status Pengiriman Terapis",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NavyPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = order.status,
                                    color = NavySecondary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Layanan: ${order.serviceName}",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Terapis: ${order.therapistName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NavyPrimary
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

                        // Actions for Client
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showChatView = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat Terapis", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { showCancelConfirmation = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Batal",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Batalkan", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Checking if there is a recently completed unrated order to show rating form!
        if (completedUnratedOrder != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(EmeraldGreenLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Done",
                                tint = MintGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Layanan Pijat Selesai!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Bagaimana pengalaman Anda bersama terapis ${completedUnratedOrder.therapistName}?",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        InteractiveStarRating(
                            rating = ratingInput,
                            onRatingChange = { ratingInput = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            placeholder = { Text("Tulis ulasan/masukan singkat...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.submitReview(ratingInput, reviewComment)
                                ratingInput = 5
                                reviewComment = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kirim Ulasan & Rating", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Hero Promo Banner Slider
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(160.dp)
                    .shadow(3.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_home_banner_1781913734961),
                        contentDescription = "PijatKu Promo Banners",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Subtly darken bottom half for typography contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Pijat Sehat, Tubuh Bugar!",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Terapis premium bersertifikat siap meluncur dalam 30 menit.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }

        // 4. Booking Quick services select Grid Header
        item {
            Text(
                text = "Pilih Layanan Jasa Pijat",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = NavyPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // List services
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                val grouped = viewModel.services.chunked(2)
                grouped.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { service ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 5.dp)
                                    .shadow(1.dp, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectedService.value = service
                                        showBookingForm = true
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                    val iconBgColor = when {
                                        service.name.contains("Tradisional") -> Color(0xFFEFF6FF)
                                        service.name.contains("Refleksi") -> Color(0xFFF0FDF4)
                                        service.name.contains("Kesehatan") -> Color(0xFFFEF2F2)
                                        service.name.contains("Relaksasi") -> Color(0xFFFAF5FF)
                                        service.name.contains("Olahraga") -> Color(0xFFFEF3C7)
                                        service.name.contains("Lansia") -> Color(0xFFFCE7F3)
                                        service.name.contains("Bekam") -> Color(0xFFF0FDFA)
                                        else -> Color(0xFFEFF6FF)
                                    }
                                    val iconTint = when {
                                        service.name.contains("Tradisional") -> NavySecondary
                                        service.name.contains("Refleksi") -> Color(0xFF22C55E)
                                        service.name.contains("Kesehatan") -> Color(0xFFEF4444)
                                        service.name.contains("Relaksasi") -> Color(0xFF8B5CF6)
                                        service.name.contains("Olahraga") -> Color(0xFFD97706)
                                        service.name.contains("Lansia") -> Color(0xFFDB2777)
                                        service.name.contains("Bekam") -> Color(0xFF0D9488)
                                        else -> NavySecondary
                                    }
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(iconBgColor, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    service.name.contains("Tradisional") -> Icons.Default.SelfImprovement
                                                    service.name.contains("Refleksi") -> Icons.Default.DirectionsWalk
                                                    service.name.contains("Kesehatan") -> Icons.Default.HealthAndSafety
                                                    service.name.contains("Relaksasi") -> Icons.Default.Spa
                                                    service.name.contains("Olahraga") -> Icons.Default.FitnessCenter
                                                    service.name.contains("Lansia") -> Icons.Default.VolunteerActivism
                                                    service.name.contains("Bekam") -> Icons.Default.LocalHospital
                                                    else -> Icons.Default.BubbleChart
                                                },
                                                contentDescription = service.name,
                                                tint = iconTint,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = service.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = currencyFormatter.format(service.basePrice),
                                            color = MintGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Promotional Vouchers Section
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Voucher Promo & Diskon 🎫",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavyPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.allVouchers.value) { voucher ->
                        Card(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable {
                                    viewModel.applyPromoCode(voucher.code)
                                    promoInputText = voucher.code
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MintGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(MintGreen, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = voucher.code,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Diskon Rp ${voucher.discountAmount.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = voucher.description,
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Nearby Therapists List Component
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daftar Terapis Profesional PijatKu 💆‍♂️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Terapis berlisensi resmi dengan ulasan terpercaya",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    
                    // Toggle Peta
                    Button(
                        onClick = { 
                            showMapMode = !showMapMode
                            selectedMapTherapist = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showMapMode) NavySecondary else NavyPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (showMapMode) Icons.Default.List else Icons.Default.Map,
                            contentDescription = "Toggle Map",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showMapMode) "Daftar" else "Peta",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Horizontal filter, search and rating controls
        // 7. Search Input Component
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama, keahlian, atau daerah...", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = NavyPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Bersihkan", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
        }

        // 8. Filters Component (Status, Massage Type, Rating Level)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Filter Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spesialisasi & Rating",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyPrimary,
                        letterSpacing = 0.5.sp
                    )
                    if (searchQuery.isNotEmpty() || selectedMassageType != "Semua" || minRating > 0.0 || showOnlyActive) {
                        Text(
                            text = "Reset Filter",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                selectedMassageType = "Semua"
                                minRating = 0.0
                                showOnlyActive = false
                            }
                        )
                    }
                }

                // First row: Online Status Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Semua Terapis
                    Card(
                        modifier = Modifier
                            .clickable { showOnlyActive = false }
                            .shadow(if (!showOnlyActive) 1.dp else 0.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!showOnlyActive) NavyPrimary else Color.White
                        ),
                        border = if (showOnlyActive) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)) else null
                    ) {
                        Text(
                            text = "Semua Terapis (${therapists.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!showOnlyActive) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Online Sahaja
                    val activeCount = therapists.count { it.isOnline }
                    Card(
                        modifier = Modifier
                            .clickable { showOnlyActive = true }
                            .shadow(if (showOnlyActive) 1.dp else 0.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (showOnlyActive) NavyPrimary else Color.White
                        ),
                        border = if (!showOnlyActive) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MintGreen, CircleShape)
                            )
                            Text(
                                text = "Aktif Online ($activeCount)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showOnlyActive) Color.White else Color.Gray
                            )
                        }
                    }
                }

                // Second row: Group Specialties in a beautiful LazyRow
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val specialtyOptions = listOf("Semua", "Tradisional", "Refleksi", "Relaksasi", "Ibu & Anak")
                    items(specialtyOptions) { type ->
                        val isSelected = selectedMassageType == type
                        Card(
                            modifier = Modifier
                                .clickable { selectedMassageType = type }
                                .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NavyPrimary else Color.White
                            ),
                            border = if (!isSelected) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)) else null
                        ) {
                            Text(
                                text = type,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Third row: Rating Filter Options
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ratingOptions = listOf(
                        0.0 to "Semua Rating ★",
                        4.5 to "4.5+ ★ Sangat Baik",
                        4.8 to "4.8+ ★ Mitra Utama"
                    )
                    items(ratingOptions) { (valRating, label) ->
                        val isSelected = minRating == valRating
                        Card(
                            modifier = Modifier
                                .clickable { minRating = valRating }
                                .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NavyPrimary else Color.White
                            ),
                            border = if (!isSelected) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (valRating > 0.0) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Render List
        if (showMapMode) {
            item {
                var mapSearchText by remember { mutableStateOf("") }
                var isSearchingMap by remember { mutableStateOf(false) }
                var searchError by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = mapSearchText,
                        onValueChange = { 
                            mapSearchText = it
                            searchError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cari Lokasi Alamat") },
                        placeholder = { Text("Contoh: Monas, Jakarta") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Ikon Cari")
                        },
                        trailingIcon = {
                            if (mapSearchText.isNotEmpty()) {
                                IconButton(onClick = { mapSearchText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            focusedLabelColor = NavyPrimary,
                            cursorColor = NavyPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearchingMap) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NavyPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mencari...", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else if (searchError != null) {
                            Text(
                                text = searchError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = {
                                if (mapSearchText.isNotBlank()) {
                                    scope.launch {
                                        isSearchingMap = true
                                        searchError = null
                                        val result = geocodeAddress(mapSearchText)
                                        isSearchingMap = false
                                        if (result != null) {
                                            viewModel.customerLat.value = result.first
                                            viewModel.customerLng.value = result.second
                                        } else {
                                            searchError = "Lokasi tidak ditemukan!"
                                        }
                                    }
                                }
                            },
                            enabled = !isSearchingMap && mapSearchText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Cari", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val mapView = rememberMapViewWithLifecycle()
                            val userLatFlow = viewModel.customerLat.collectAsState().value
                            val userLngFlow = viewModel.customerLng.collectAsState().value

                            AndroidView(
                                factory = { 
                                    mapView.apply {
                                        setMultiTouchControls(true)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { map ->
                                    map.overlays.clear()
                                    
                                    val userPoint = GeoPoint(userLatFlow, userLngFlow)
                                    val userMarker = Marker(map).apply {
                                        position = userPoint
                                        title = "Lokasi Saya"
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    }
                                    map.overlays.add(userMarker)
                                    
                                    filteredTherapists.forEach { therapist ->
                                        val hash = therapist.id.hashCode().toDouble() / Int.MAX_VALUE
                                        val tLat = userLatFlow + hash * 0.012
                                        val tLng = userLngFlow + Math.sin(hash) * 0.012
                                        val tPoint = GeoPoint(tLat, tLng)
                                        
                                        val therapistMarker = Marker(map).apply {
                                            position = tPoint
                                            title = therapist.name
                                            subDescription = "Rating: ${therapist.rating} ★ (${therapist.totalReviews} ulasan)"
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            
                                            setOnMarkerClickListener { marker, _ ->
                                                selectedMapTherapist = therapist
                                                marker.showInfoWindow()
                                                true
                                            }
                                        }
                                        map.overlays.add(therapistMarker)
                                    }
                                    
                                    map.controller.setCenter(userPoint)
                                    map.controller.setZoom(13.5)
                                    map.invalidate()
                                }
                            )
                        }
                    }
                }
            }

            if (selectedMapTherapist != null) {
                val t = selectedMapTherapist!!
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                            .size(45.dp)
                                            .background(NavyPrimary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = t.name.firstOrNull()?.toString() ?: "T",
                                            color = NavyPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = t.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Rating",
                                                tint = AccentGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${t.rating} (${t.totalReviews} ulasan)",
                                                fontSize = 12.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (t.isOnline) MintGreen.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (t.isOnline) "ONLINE" else "OFFLINE",
                                        color = if (t.isOnline) MintGreen else Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Spesialisasi: ${getTherapistSpecialty(t.name)}",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { selectedMapTherapist = null },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                                ) {
                                    Text("Tutup", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.selectedTherapist.value = t
                                        showBookingForm = true
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Pesan Terapis Ini", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySecondary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = NavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Silakan sentuh pin terapis hijau di peta untuk melihat detail spesialisasi dan memesan secara instan.",
                                fontSize = 11.sp,
                                color = NavyPrimary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        } else if (filteredTherapists.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (showOnlyActive) "Maaf, saat ini tidak ada terapis aktif online." else "Maaf, belum ada mitra terapis yang terdaftar saat ini.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(filteredTherapists) { therapist ->
                // Dynamic specialty & description based on therapist name
                val specialty = when {
                    therapist.name.contains("Budi", ignoreCase = true) -> "Pijat Tradisional Jawa, Urut Capek, Refleksi Saraf Kaki"
                    therapist.name.contains("Ani", ignoreCase = true) -> "Pijat Ibu & Anak, Totok Aura Wajah, Pijat Relaksasi Swedia"
                    therapist.name.contains("Joko", ignoreCase = true) -> "Pijat Aromaterapi Kesehatan, Bekam Kering, Deep Tissue Therapy"
                    else -> "Terapi Kebal Pegal, Pijat Kebugaran, Kop Masuk Angin"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar Layout with Available Badge
                            Box(modifier = Modifier.size(54.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(NavyPrimary.copy(alpha = 0.1f), MintGreen.copy(alpha = 0.15f))
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = therapist.name.take(1),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = NavyPrimary
                                    )
                                }
                                
                                // Glowing Online Badge
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(if (therapist.isOnline) MintGreen else Color.Gray, CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Therapist Info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = therapist.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NavyPrimary
                                    )
                                    
                                    // Certified Label
                                    Box(
                                        modifier = Modifier
                                            .background(MintGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Mitra Pro",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = NavyPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Ratings and Reviews ulasan count
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = AccentGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "%.1f".format(therapist.rating).replace(",", "."),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "• ${therapist.totalReviews} ulasan",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "• ${therapist.customerCount} order",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Expertise
                                Text(
                                    text = "Keahlian Spesialis:",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = specialty,
                                    fontSize = 11.sp,
                                    color = NavySecondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color.LightGray.copy(alpha = 0.4f)
                        )

                        // Base Pricing and Order Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Tarif Layanan",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Mulai Rp 100.000",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16A34A)
                                )
                            }

                            // Explicit 'Order Now' button
                            val isAvailable = therapist.isOnline
                            Button(
                                onClick = {
                                    viewModel.selectedTherapist.value = therapist
                                    if (viewModel.selectedService.value == null) {
                                        viewModel.selectedService.value = viewModel.services.first()
                                    }
                                    showBookingForm = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAvailable) MintGreen else Color.LightGray,
                                    contentColor = if (isAvailable) Color.White else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MedicalServices,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isAvailable) "Pesan Sekarang" else "Offline / Sibuk",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    } else {
            BookingHistoryView(
                viewModel = viewModel,
                currencyFormatter = currencyFormatter,
                onRateOrderClick = {
                    ratingInput = 5
                    reviewComment = ""
                    activeTab = "home"
                }
            )
        }

        // Float customized beautiful bottom Navigation Bar
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(0.9f)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { activeTab = "home" }
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Beranda",
                        tint = if (activeTab == "home") MintGreen else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Beranda",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "home") MintGreen else Color.Gray
                    )
                }

                // History Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            activeTab = "history"
                            viewModel.fetchBookingHistoryFromFirestore()
                        }
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Riwayat",
                        tint = if (activeTab == "history") MintGreen else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Riwayat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "history") MintGreen else Color.Gray
                    )
                }
            }
        }
    }

    // Interactive booking Form overlay dialog
    if (showBookingForm) {
        val serv = viewModel.selectedService.collectAsState().value ?: viewModel.services.first()
        val ther = viewModel.selectedTherapist.collectAsState().value ?: therapists.firstOrNull { it.isOnline }

        AlertDialog(
            onDismissRequest = { showBookingForm = false },
            title = {
                Text(
                    text = "Konfirmasi Pemesanan 💆‍♂️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NavyPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Sesuaikan spesifikasi pesanan Anda sebelum memanggil terapis.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    // Selected layout details
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Layanan: ${serv.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Durasi: ${serv.durationMin} Menit",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Terapis: ${ther?.name ?: "Terapis Pilihan Terdekat"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavySecondary
                            )
                            Text(
                                text = "Lokasi Penjemputan GPS Terdeteksi",
                                fontSize = 10.sp,
                                color = MintGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Input Address field
                    OutlinedTextField(
                        value = viewModel.customAddress.collectAsState().value,
                        onValueChange = { viewModel.customAddress.value = it },
                        label = { Text("Alamat Lengkap Rumah", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Promo input Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promoInputText,
                            onValueChange = { promoInputText = it },
                            placeholder = { Text("KODE PROMO", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                viewModel.applyPromoCode(promoInputText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan", fontSize = 11.sp)
                        }
                    }

                    if (appliedVoucher != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFD1FAE5), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalActivity,
                                tint = MintGreen,
                                contentDescription = "Coupon"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Promo ${appliedVoucher!!.code} Aktif: Hemat Rp ${appliedVoucher!!.discountAmount.toInt()}",
                                fontSize = 10.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Payment selection Method:
                    val paymentMethod by viewModel.selectedPaymentMethod.collectAsState()
                    Text(
                        text = "Metode Pembayaran:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NavyPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "QRIS", "TRANSFER", "EWALLET").forEach { method ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (paymentMethod == method) MintGreen else Color(0xFFE2E8F0))
                                    .clickable { viewModel.selectedPaymentMethod.value = method }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method,
                                    color = if (paymentMethod == method) Color.White else Color.DarkGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Bottom Summary math
                    val configVal = viewModel.adminConfig.collectAsState().value
                    val basePriceVal = serv.basePrice
                    val feeVal = configVal.serviceFee
                    val discAmount = appliedVoucher?.discountAmount ?: 0.0
                    val totalSum = (basePriceVal + feeVal - discAmount).coerceAtLeast(0.0)

                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal Layanan:", fontSize = 11.sp, color = Color.Gray)
                        Text(currencyFormatter.format(basePriceVal), fontSize = 11.sp, color = Color.Gray)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Biaya Platform & App:", fontSize = 11.sp, color = Color.Gray)
                        Text(currencyFormatter.format(feeVal), fontSize = 11.sp, color = Color.Gray)
                    }
                    if (discAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Potongan Diskon:", fontSize = 11.sp, color = MintGreen)
                            Text("- " + currencyFormatter.format(discAmount), fontSize = 11.sp, color = MintGreen)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pembayaran:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                        Text(
                            currencyFormatter.format(totalSum),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MintGreen
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.placeBooking()
                        showBookingForm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Pesan Sekarang & Antar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookingForm = false }) {
                    Text("Kembali", color = Color.Gray, fontSize = 12.sp)
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
                    text = "Obrolan dengan Terapis 💬",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavyPrimary
                )
            },
            text = {
                Column {
                    // Chat thread
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
                                    text = "Obrolan kosong. Mulai chat untuk koordinasi penjemputan terapis.",
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
                            placeholder = { Text("Ketik pesan...", fontSize = 11.sp) },
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

@Composable
fun FirebaseAuthScreen(
    viewModel: PijatKuViewModel,
    onDemoModeClick: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NavyPrimary, NavySecondary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Application Logo / Header
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "PijatKu Logo",
                        tint = MintGreen,
                        modifier = Modifier
                            .size(64.dp)
                            .background(MintGreen.copy(alpha = 0.1f), CircleShape)
                            .padding(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isSignUp) "Daftar Akun PijatKu" else "Masuk ke PijatKu",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = if (isSignUp) "Hubungkan langsung dengan Firebase Authentication" else "Masukkan detail akun Firebase Anda",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFF7F1D1D),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (successMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = successMessage ?: "",
                                color = Color(0xFF065F46),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Form Fields
                    if (isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Lengkap") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NavyPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor Telepon") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NavyPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Alamat Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NavyPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NavyPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isSignUp) {
                        OutlinedTextField(
                            value = referralCode,
                            onValueChange = { referralCode = it },
                            label = { Text("Kode Referral (Opsional)") },
                            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = NavyPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Loading indicator / Action button
                    if (isLoading) {
                        CircularProgressIndicator(color = MintGreen, modifier = Modifier.padding(12.dp))
                    } else {
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Email dan password wajib diisi."
                                    return@Button
                                }
                                if (isSignUp && name.isBlank()) {
                                    errorMessage = "Nama wajib diisi untuk registrasi."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                successMessage = null

                                if (isSignUp) {
                                    viewModel.registerWithFirebase(
                                        email = email,
                                        password = password,
                                        name = name,
                                        phone = phone,
                                        referralCode = referralCode,
                                        onSuccess = {
                                            isLoading = false
                                            successMessage = "Akun berhasil didaftarkan!"
                                        },
                                        onError = { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                } else {
                                    viewModel.loginWithFirebase(
                                        email = email,
                                        password = password,
                                        onSuccess = {
                                            isLoading = false
                                            successMessage = "Berhasil masuk!"
                                        },
                                        onError = { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text(
                                text = if (isSignUp) "DAFTAR SEKARANG" else "MASUK SECARA AMAN",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch Mode Toggle
                    TextButton(
                        onClick = {
                            isSignUp = !isSignUp
                            errorMessage = null
                            successMessage = null
                        }
                    ) {
                        Text(
                            text = if (isSignUp) "Sudah punya akun? Masuk di sini" else "Belum punya akun? Daftar gratis",
                            color = NavyPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    val customBorder = BorderStroke(1.5.dp, MintGreen)
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.LightGray.copy(alpha = 0.5f)
                    )

                    // Skip / Bypass Button
                    OutlinedButton(
                        onClick = onDemoModeClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = customBorder,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MintGreen)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gunakan Mode Simulasi Demo",
                            fontWeight = FontWeight.Bold,
                            color = MintGreen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryView(
    viewModel: PijatKuViewModel,
    currencyFormatter: NumberFormat,
    onRateOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firestoreOrders by viewModel.firestoreOrders.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    var subTab by remember { mutableStateOf("active") } // "active" or "history"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // 1. Header (Navy Gradient Banner matching the application's overall look)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Riwayat Pemesanan",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Lihat status pesanan pijat mendatang dan riwayat transaksi masa lalu Anda",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Segmented Sub-Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("active" to "💆‍♂️ Mendatang", "history" to "📜 Riwayat").forEach { (tabId, tabName) ->
                val isSelected = subTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MintGreen else Color(0xFFE2E8F0))
                        .clickable { subTab = tabId }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual Refresh Icon button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (subTab == "active") "Sesi Aktif & Mendatang" else "Riwayat Selesai & Batal",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = NavyPrimary
            )

            IconButton(onClick = { viewModel.fetchBookingHistoryFromFirestore() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MintGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 3. Render List of Bookings from Firestore
        if (isHistoryLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MintGreen)
            }
        } else {
            // Filter orders based on the sub-tab selection
            val filteredList = firestoreOrders.filter { order ->
                if (subTab == "active") {
                    order.status != "SELESAI" && order.status != "BATAL"
                } else {
                    order.status == "SELESAI" || order.status == "BATAL"
                }
            }

            if (filteredList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📭",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (subTab == "active") "Tidak ada pemesanan aktif saat ini" else "Belum ada riwayat transaksi",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gunakan tombol pesan untuk memanggil terapis pijat.",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp)
                ) {
                    items(filteredList) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFEFF6FF), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("💆‍♂️", fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = order.serviceName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = NavyPrimary
                                            )
                                            Text(
                                                text = "Bersama: ${order.therapistName}",
                                                color = NavySecondary,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Status Badge styling based on order.status
                                    val (badgeBgColor, badgeContentColor, statusLabel) = when (order.status) {
                                        "MENUNGGU" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Menunggu")
                                        "MENUJU_LOKASI" -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "OTW")
                                        "TIBA" -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "Tiba")
                                        "MELAYANI" -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), "Melayani")
                                        "SELESAI" -> Triple(Color(0xFFD1FAE5), Color(0xFF10B981), "Selesai")
                                        "BATAL" -> Triple(Color(0xFFFEE2E2), Color(0xFFEF4444), "Batal")
                                        else -> Triple(Color(0xFFE2E8F0), Color(0xFF475569), order.status)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeBgColor)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            color = badgeContentColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Divider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = Color.LightGray.copy(alpha = 0.4f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Tanggal",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${order.date} • ${order.time}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = "Alamat",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = order.address,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Biaya Total",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = currencyFormatter.format(order.price),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = MintGreen
                                        )
                                    }
                                }

                                // Interactive Rating Row if Completed & Unrated
                                if (order.status == "SELESAI" && order.rating == 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onRateOrderClick() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                            Text(
                                                text = "Beri Ulasan & Rating",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else if (order.status == "SELESAI" && order.rating > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFEF3C7).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                            (1..5).forEach { i ->
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (i <= order.rating) Color(0xFFF59E0B) else Color.LightGray,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                        if (order.reviewComment.isNotEmpty()) {
                                            Text(
                                                text = "\"${order.reviewComment}\"",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                maxLines = 1
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
    }
}
