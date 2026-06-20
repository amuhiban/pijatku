package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
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
            // Set first currentUser as customer by default
            loginAs("cust_ahmad")
            observeNotifications()
            observeActiveOrder()
        }
    }

    fun loginAs(userId: String) {
        viewModelScope.launch {
            val user = repository.getUserSync(userId)
            if (user != null) {
                currentUser.value = user
                currentRole.value = user.role
                observeNotifications()
            }
        }
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

    fun placeBooking() {
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
            date = "Hari ini",
            time = "Sekarang (Segera)",
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
            val ordersList = repository.allOrders.firstOrNull() ?: emptyList()
            // Find the most recent complete order that has rating = 0
            val unrated = ordersList.firstOrNull { it.customerId == "cust_ahmad" && it.status == "SELESAI" && it.rating == 0 }
            if (unrated != null) {
                repository.rateOrder(unrated.id, rating, comment)

                // Recalculate average therapist rating & reviews
                val tId = unrated.therapistId
                val tOrders = ordersList.filter { it.therapistId == tId && (it.id == unrated.id || it.rating > 0) }
                val ratedCount = tOrders.count { it.rating > 0 } + 1
                val totalStars = tOrders.sumOf { if (it.id == unrated.id) rating else it.rating }
                val avgRating = totalStars.toFloat() / ratedCount

                val therapistUser = repository.getUserSync(tId)
                if (therapistUser != null) {
                    val updatedTherapist = therapistUser.copy(
                        rating = avgRating,
                        totalReviews = ratedCount,
                        customerCount = therapistUser.customerCount + 1
                    )
                    repository.insertUser(updatedTherapist)
                }
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
