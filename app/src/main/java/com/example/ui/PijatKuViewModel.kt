package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MassageService(
    val name: String,
    val description: String,
    val basePrice: Double,
    val durationMin: Int
)

class PijatKuViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = PijatKuRepository(db)

    // Simulation settings
    val currentRole = MutableStateFlow("CUSTOMER") // "CUSTOMER", "THERAPIST", "ADMIN"
    val currentUser = MutableStateFlow<UserEntity?>(null)

    // Loaded states
    val allTherapists: StateFlow<List<UserEntity>> = repository.allTherapists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allOrders: StateFlow<List<OrderEntity>> = repository.allOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val adminConfig: StateFlow<ConfigEntity> = repository.adminConfig
        .map { it ?: ConfigEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConfigEntity()
        )

    val allVouchers: StateFlow<List<VoucherEntity>> = repository.allVouchers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic states for active ordering flow
    val selectedService = MutableStateFlow<MassageService?>(null)
    val selectedTherapist = MutableStateFlow<UserEntity?>(null)
    val appliedVoucher = MutableStateFlow<VoucherEntity?>(null)
    val customAddress = MutableStateFlow("Jl. Sudirman No. 12, Jakarta Pusat")
    val selectedPaymentMethod = MutableStateFlow("CASH") // "QRIS", "TRANSFER", "EWALLET", "CASH"

    val firestoreOrders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val isHistoryLoading = MutableStateFlow(false)

    fun fetchBookingHistoryFromFirestore() {
        val userVal = currentUser.value ?: return
        isHistoryLoading.value = true
        FirebaseSyncManager.fetchOrdersFromFirestore(
            customerId = userVal.id,
            onSuccess = { orders ->
                firestoreOrders.value = orders
                isHistoryLoading.value = false
            },
            onFailure = { error ->
                Log.e("PijatKuViewModel", "Error fetching Firestore orders: ${error.message}", error)
                // Fallback: populate from allOrders state flow directly
                val localOrders = allOrders.value.filter { it.customerId == userVal.id }
                firestoreOrders.value = localOrders.sortedByDescending { it.timestamp }
                isHistoryLoading.value = false
            }
        )
    }

    fun refreshTherapistRatingsFromFirestore() {
        viewModelScope.launch {
            FirebaseSyncManager.fetchTherapistsFromFirestore { fsTherapists ->
                viewModelScope.launch {
                    for (ft in fsTherapists) {
                        val localTherapist = repository.getUserSync(ft.id)
                        if (localTherapist != null) {
                            val updated = localTherapist.copy(
                                rating = ft.rating,
                                totalReviews = ft.totalReviews
                            )
                            repository.insertUser(updated)
                        }
                    }
                }
            }
        }
    }

    // Map Coordinates tracking
    val customerLat = MutableStateFlow(-6.2088)
    val customerLng = MutableStateFlow(106.8456)
    val therapistLat = MutableStateFlow(-6.2120)
    val therapistLng = MutableStateFlow(106.8400)

    val activeOrder = MutableStateFlow<OrderEntity?>(null)
    val currentChatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val customerNotifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val therapistNotifications = MutableStateFlow<List<NotificationEntity>>(emptyList())

    // List of massage services
    val services = listOf(
        MassageService("Pijat Tradisional", "Pijat urat seluruh badan untuk meredakan pegal linu", 120000.0, 90),
        MassageService("Pijat Refleksi", "Fokus area telapak kaki dan tangan untuk melancarkan sirkulasi", 90000.0, 60),
        MassageService("Pijat Kesehatan", "Pijat khusus dengan stimulasi poin penting tubuh", 130000.0, 90),
        MassageService("Pijat Relaksasi", "Kombinasi aromaterapi lembut untuk melepaskan stres pikiran", 140000.0, 90),
        MassageService("Pijat Olahraga", "Mengurangi ketegangan otot dalam pasca olahraga berat", 150000.0, 60),
        MassageService("Pijat Lansia", "Pijatan sangat lambat dan lembut yang aman untuk lansia", 100000.0, 60),
        MassageService("Terapi Bekam", "Terapi bekam kering/basah mengeluarkan darah kotor & toksin", 110000.0, 90),
        MassageService("Terapi Akupresur", "Tekanan ibu jari pada titik meridian untuk penyembuhan alami", 125000.0, 60)
    )

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            // Initial Firebase Sync and check authed user
            val authUser = try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                null
            }
            if (authUser != null) {
                val dbUser = repository.getUserSync(authUser.uid)
                if (dbUser != null) {
                    currentUser.value = dbUser
                    currentRole.value = dbUser.role
                } else {
                    val newUser = UserEntity(
                        id = authUser.uid,
                        role = "CUSTOMER",
                        name = authUser.displayName ?: authUser.email?.substringBefore("@") ?: "Pelanggan PijatKu",
                        email = authUser.email ?: "",
                        phone = "",
                        profileImageUrl = "cust_avatar",
                        balance = 500000.0,
                        referralCode = "REF-${authUser.uid.take(5).uppercase()}",
                        status = "APPROVED"
                    )
                    repository.insertUser(newUser)
                    currentUser.value = newUser
                    currentRole.value = "CUSTOMER"
                }
            } else {
                // Set first currentUser as customer by default
                loginAs("cust_ahmad")
            }
            observeNotifications()
            observeActiveOrder()
            refreshTherapistRatingsFromFirestore()
        }
    }

    fun loginAs(userId: String) {
        viewModelScope.launch {
            val user = repository.getUserSync(userId)
            if (user != null) {
                currentUser.value = user
                currentRole.value = user.role
                observeNotifications()
                refreshTherapistRatingsFromFirestore()
            }
        }
    }

    fun isUserLoggedInWithFirebase(): Boolean {
        return try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    fun registerWithFirebase(
        email: String,
        password: String,
        name: String,
        phone: String,
        referralCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val authUser = task.result?.user
                            if (authUser != null) {
                                viewModelScope.launch {
                                    val newUser = UserEntity(
                                        id = authUser.uid,
                                        role = "CUSTOMER",
                                        name = name.ifBlank { authUser.email?.substringBefore("@") ?: "Pelanggan" },
                                        email = email,
                                        phone = phone,
                                        profileImageUrl = "cust_avatar",
                                        balance = 500000.0,
                                        referralCode = referralCode.ifBlank { "REF-${authUser.uid.take(5).uppercase()}" },
                                        status = "APPROVED"
                                    )
                                    repository.insertUser(newUser)
                                    currentUser.value = newUser
                                    currentRole.value = "CUSTOMER"
                                    observeNotifications()
                                    onSuccess()
                                }
                            } else {
                                onError("Gagal mendapatkan data user setelah registrasi.")
                            }
                        } else {
                            onError(task.exception?.localizedMessage ?: "Registrasi gagal.")
                        }
                    }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Terjadi kesalahan.")
            }
        }
    }

    fun loginWithFirebase(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val authUser = task.result?.user
                            if (authUser != null) {
                                viewModelScope.launch {
                                    var dbUser = repository.getUserSync(authUser.uid)
                                    if (dbUser == null) {
                                        dbUser = UserEntity(
                                            id = authUser.uid,
                                            role = "CUSTOMER",
                                            name = authUser.displayName ?: authUser.email?.substringBefore("@") ?: "Pelanggan PijatKu",
                                            email = authUser.email ?: email,
                                            phone = "",
                                            profileImageUrl = "cust_avatar",
                                            balance = 500000.0,
                                            referralCode = "REF-${authUser.uid.take(5).uppercase()}",
                                            status = "APPROVED"
                                        )
                                        repository.insertUser(dbUser)
                                    }
                                    currentUser.value = dbUser
                                    currentRole.value = dbUser.role
                                    observeNotifications()
                                    onSuccess()
                                }
                            } else {
                                onError("Gagal memuat data user.")
                            }
                        } else {
                            onError(task.exception?.localizedMessage ?: "Login gagal. Silakan periksa email dan password.")
                        }
                    }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Terjadi kesalahan.")
            }
        }
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // ignore
        }
        loginAs("cust_ahmad") // fallback to default simulation user
    }

    private fun observeNotifications() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.getNotifications(user.id).collect { list ->
                if (user.role == "CUSTOMER") {
                    customerNotifications.value = list
                } else if (user.role == "THERAPIST") {
                    therapistNotifications.value = list
                }
            }
        }
    }

    private fun observeActiveOrder() {
        viewModelScope.launch {
            allOrders.collect { orders ->
                // Look for an order that is not FINISHED or CANCELLED
                val ongoing = orders.firstOrNull { it.status != "SELESAI" && it.status != "BATAL" }
                activeOrder.value = ongoing

                if (ongoing != null) {
                    // Update chat messages reactively
                    repository.getChatMessages(ongoing.id).collect { messages ->
                        currentChatMessages.value = messages
                    }
                } else {
                    currentChatMessages.value = emptyList()
                }
            }
        }
    }

    // Customer Actions
    fun applyPromoCode(code: String): String {
        var status = "Kode promo tidak valid"
        viewModelScope.launch {
            val v = repository.getVoucherSync(code.uppercase())
            if (v != null) {
                appliedVoucher.value = v
                status = "Promo ${v.code} berhasil dipasang!"
            }
        }
        return status
    }

    fun removePromoCode() {
        appliedVoucher.value = null
    }

    fun placeBooking(bookingDate: String = "Hari ini", bookingTime: String = "Sekarang (Segera)") {
        val customer = currentUser.value ?: return
        val therapist = selectedTherapist.value ?: allTherapists.value.firstOrNull { it.isOnline && it.status == "APPROVED" } ?: return
        val service = selectedService.value ?: services.first()
        val basePrice = service.basePrice + adminConfig.value.serviceFee
        val discount = appliedVoucher.value?.discountAmount ?: 0.0
        val finalPrice = (basePrice - discount).coerceAtLeast(0.0)

        val newOrder = OrderEntity(
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            therapistId = therapist.id,
            therapistName = therapist.name,
            serviceName = service.name,
            price = finalPrice,
            date = bookingDate,
            time = bookingTime,
            status = "MENUNGGU",
            paymentMethod = selectedPaymentMethod.value,
            paymentStatus = if (selectedPaymentMethod.value == "CASH") "PENDING" else "SUCCESS",
            address = customAddress.value,
            latitude = customerLat.value,
            longitude = customerLng.value
        )

        viewModelScope.launch {
            val orderId = repository.insertOrder(newOrder).toInt()

            // Reset active picker
            selectedService.value = null
            selectedTherapist.value = null
            appliedVoucher.value = null

            // Notify Therapist
            repository.insertNotification(NotificationEntity(
                title = "Order Masuk Baru! 💆‍♂️",
                description = "Pelanggan ${customer.name} memesan ${service.name} ke ${newOrder.address}.",
                relativeTo = "THERAPIST",
                relativeId = therapist.id
            ))

            // Simulate therapist heading to location automatically after 10s if we want to show reactive transition,
            // or let the user switch roles and handle it manually!
        }
    }

    // Shared / Therapist Actions
    fun advanceOrderStatus() {
        val curOrder = activeOrder.value ?: return
        val nextStatus = when (curOrder.status) {
            "MENUNGGU" -> "MENUJU_LOKASI"
            "MENUJU_LOKASI" -> "TIBA"
            "TIBA" -> "MELAYANI"
            "MELAYANI" -> "SELESAI"
            else -> curOrder.status
        }

        viewModelScope.launch {
            repository.updateOrderStatus(curOrder.id, nextStatus)

            // Notifications
            val title = when (nextStatus) {
                "MENUJU_LOKASI" -> "Terapis OTW! 🏃‍♂️"
                "TIBA" -> "Terapis Sudah Sampai! 🚪"
                "MELAYANI" -> "Terapi Dimulai! 💆‍♂️"
                "SELESAI" -> "Layanan Pijat Selesai! 🎉"
                else -> ""
            }

            val desc = when (nextStatus) {
                "MENUJU_LOKASI" -> "Terapis ${curOrder.therapistName} sedang menuju ke lokasi Anda."
                "TIBA" -> "Terapis ${curOrder.therapistName} telah tiba di titik penjemputan."
                "MELAYANI" -> "Layanan ${curOrder.serviceName} sedang berlangsung, silakan nikmati."
                "SELESAI" -> "Terima kasih telah menggunakan PijatKu! Jangan lupa beri ulasan terbaik."
                else -> ""
            }

            if (title.isNotEmpty()) {
                repository.insertNotification(NotificationEntity(
                    title = title,
                    description = desc,
                    relativeTo = "CUSTOMER",
                    relativeId = curOrder.customerId
                ))
            }

            // Distribute balances if status is SELESAI
            if (nextStatus == "SELESAI") {
                // Update payment status to success
                repository.updateOrderPaymentStatus(curOrder.id, "SUCCESS")

                // Commision math
                val totalPrice = curOrder.price
                val commPercent = adminConfig.value.platformCommissionPercent
                val therapistShareRatio = (100f - commPercent) / 100f
                val therapistEarnings = totalPrice * therapistShareRatio

                // Add to therapist wallet
                val currentTherapist = repository.getUserSync(curOrder.therapistId)
                if (currentTherapist != null) {
                    repository.updateUserBalance(curOrder.therapistId, currentTherapist.balance + therapistEarnings)
                }

                // If paid online via E-Wallet/QRIS, deduct from customer profile balance (simulation)
                if (curOrder.paymentMethod != "CASH") {
                    val currentCust = repository.getUserSync(curOrder.customerId)
                    if (currentCust != null) {
                        repository.updateUserBalance(curOrder.customerId, (currentCust.balance - totalPrice).coerceAtLeast(0.0))
                    }
                }
            }

            // Simple map simulation: bring therapist coordinates closer to customer's as they advance
            if (nextStatus == "MENUJU_LOKASI") {
                therapistLat.value = curOrder.latitude - 0.001
                therapistLng.value = curOrder.longitude + 0.001
            } else if (nextStatus == "TIBA") {
                therapistLat.value = curOrder.latitude
                therapistLng.value = curOrder.longitude
            }
        }
    }

    fun cancelOrder() {
        val curOrder = activeOrder.value ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(curOrder.id, "BATAL")
            repository.insertNotification(NotificationEntity(
                title = "Pesanan Dibatalkan ❌",
                description = "Pesanan layanan ${curOrder.serviceName} telah dibatalkan.",
                relativeTo = "CUSTOMER",
                relativeId = curOrder.customerId
            ))
            repository.insertNotification(NotificationEntity(
                title = "Pesanan Dibatalkan ❌",
                description = "Pesanan layanan oleh ${curOrder.customerName} dibatalkan.",
                relativeTo = "THERAPIST",
                relativeId = curOrder.therapistId
            ))
        }
    }

    fun sendChatMessage(text: String) {
        val curOrder = activeOrder.value ?: return
        val sender = currentUser.value ?: return
        val receiverId = if (sender.id == curOrder.customerId) curOrder.therapistId else curOrder.customerId

        if (text.trim().isEmpty()) return

        val message = ChatMessageEntity(
            orderId = curOrder.id,
            senderId = sender.id,
            receiverId = receiverId,
            message = text
        )

        viewModelScope.launch {
            repository.insertChatMessage(message)
        }
    }

    fun submitReview(rating: Int, comment: String) {
        viewModelScope.launch {
            val userId = currentUser.value?.id ?: "cust_ahmad"
            val userName = currentUser.value?.name ?: "Pelanggan PijatKu"
            val ordersList = repository.allOrders.firstOrNull() ?: emptyList()
            // Find the most recent complete order that has rating = 0
            val unrated = ordersList.firstOrNull { it.customerId == userId && it.status == "SELESAI" && it.rating == 0 }
            if (unrated != null) {
                repository.rateOrder(unrated.id, rating, comment)

                FirebaseSyncManager.submitReviewToFirestore(
                    orderId = unrated.id,
                    customerId = userId,
                    customerName = userName,
                    therapistId = unrated.therapistId,
                    rating = rating,
                    comment = comment,
                    onComplete = {
                        // Synchronize updated therapist stats back from Firestore dynamically
                        refreshTherapistRatingsFromFirestore()
                    }
                )
            }
        }
    }

    // Therapist Actions
    fun submitTherapistRegistration(
        name: String,
        phone: String,
        email: String,
        ktpName: String,
        certName: String,
        selfieName: String,
        workplaceName: String
    ) {
        val rawId = "therapist_" + name.lowercase().replace(" ", "_").take(15)
        val newTherapist = UserEntity(
            id = rawId,
            role = "THERAPIST",
            name = name,
            email = email,
            phone = phone,
            isOnline = false,
            rating = 0.0f,
            totalReviews = 0,
            balance = 0.0,
            customerCount = 0,
            ktpDoc = ktpName,
            certDoc = certName,
            selfieDoc = selfieName,
            workplaceDoc = workplaceName,
            status = "PENDING"
        )
        viewModelScope.launch {
            repository.insertUser(newTherapist)
            // Log back in as customer (the one registering the therapist) or switch
            loginAs(rawId)
        }
    }

    fun topUpCustomerBalance(amount: Double) {
        val user = currentUser.value ?: return
        if (user.role == "CUSTOMER") {
            val newBal = user.balance + amount
            viewModelScope.launch {
                repository.updateUserBalance(user.id, newBal)
                currentUser.value = user.copy(balance = newBal)
            }
        }
    }

    fun setTherapistOnline(isOnline: Boolean) {
        val user = currentUser.value ?: return
        if (user.role == "THERAPIST") {
            viewModelScope.launch {
                repository.updateTherapistOnlineStatus(user.id, isOnline)
                currentUser.value = user.copy(isOnline = isOnline)
            }
        }
    }

    fun withdrawTherapistBalance(amount: Double) {
        val user = currentUser.value ?: return
        if (user.role == "THERAPIST" && user.balance >= amount) {
            val newBal = user.balance - amount
            viewModelScope.launch {
                repository.updateUserBalance(user.id, newBal)
                currentUser.value = user.copy(balance = newBal)
            }
        }
    }

    // Admin Actions
    fun updateTherapistApproval(id: String, approve: Boolean) {
        viewModelScope.launch {
            val nextStatus = if (approve) "APPROVED" else "PENDING"
            repository.updateTherapistStatus(id, nextStatus)
        }
    }

    fun suspendUser(id: String, suspend: Boolean) {
        viewModelScope.launch {
            val nextStatus = if (suspend) "SUSPENDED" else "APPROVED"
            repository.updateTherapistStatus(id, nextStatus)
        }
    }

    fun updatePlatformConfig(commission: Float, serviceFee: Double) {
        viewModelScope.launch {
            val comp = 100f - commission
            repository.insertConfig(ConfigEntity(
                platformCommissionPercent = commission,
                therapistSharePercent = comp,
                serviceFee = serviceFee
            ))
        }
    }

    fun addVoucher(code: String, discount: Double, desc: String, isCashback: Boolean) {
        val type = if (isCashback) "CASHBACK" else "VOUCHER"
        viewModelScope.launch {
            repository.insertVoucher(VoucherEntity(
                code = code.uppercase(),
                discountAmount = discount,
                description = desc,
                type = type
            ))
        }
    }

    fun deleteVoucher(code: String) {
        viewModelScope.launch {
            repository.deleteVoucher(code)
        }
    }

    // Profile updates
    fun updateProfile(name: String, email: String, phone: String, photoName: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserProfile(user.id, name, email, phone, photoName)
            currentUser.value = user.copy(name = name, email = email, phone = phone, profileImageUrl = photoName)
        }
    }
}
