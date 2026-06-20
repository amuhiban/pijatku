package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PijatKuRepository(private val db: AppDatabase) {
    val daos = db.appDaos()

    // Flow Getters
    val allUsers: Flow<List<UserEntity>> = daos.getAllUsers()
    val allTherapists: Flow<List<UserEntity>> = daos.getAllTherapists()
    val allOrders: Flow<List<OrderEntity>> = daos.getAllOrders()
    val allVouchers: Flow<List<VoucherEntity>> = daos.getAllVouchers()
    val adminConfig: Flow<ConfigEntity?> = daos.getConfig()

    fun getUserById(id: String): Flow<UserEntity?> = daos.getUserById(id)
    fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>> = daos.getOrdersForCustomer(customerId)
    fun getOrdersForTherapist(therapistId: String): Flow<List<OrderEntity>> = daos.getOrdersForTherapist(therapistId)
    fun getOrderById(id: Int): Flow<OrderEntity?> = daos.getOrderById(id)
    fun getChatMessages(orderId: Int): Flow<List<ChatMessageEntity>> = daos.getChatMessages(orderId)
    fun getNotifications(userId: String): Flow<List<NotificationEntity>> = daos.getNotifications(userId)

    suspend fun getUserSync(id: String) = daos.getUserSync(id)
    suspend fun getOrderSync(id: Int) = daos.getOrderSync(id)

    // Writers
    suspend fun insertUser(user: UserEntity) = daos.insertUser(user)
    suspend fun updateTherapistOnlineStatus(id: String, isOnline: Boolean) = daos.updateTherapistOnlineStatus(id, isOnline)
    suspend fun updateUserBalance(id: String, balance: Double) = daos.updateUserBalance(id, balance)
    suspend fun updateTherapistStatus(id: String, status: String) = daos.updateTherapistStatus(id, status)
    suspend fun updateUserProfile(id: String, name: String, email: String, phone: String, imageUrl: String) =
        daos.updateUserProfile(id, name, email, phone, imageUrl)

    suspend fun insertOrder(order: OrderEntity): Long = daos.insertOrder(order)
    suspend fun updateOrderStatus(id: Int, status: String) = daos.updateOrderStatus(id, status)
    suspend fun rateOrder(id: Int, rating: Int, comment: String) = daos.rateOrder(id, rating, comment)
    suspend fun updateOrderPaymentStatus(id: Int, pStatus: String) = daos.updateOrderPaymentStatus(id, pStatus)

    suspend fun insertChatMessage(msg: ChatMessageEntity) = daos.insertChatMessage(msg)
    suspend fun getVoucherSync(code: String) = daos.getVoucherSync(code)
    suspend fun insertVoucher(voucher: VoucherEntity) = daos.insertVoucher(voucher)
    suspend fun deleteVoucher(code: String) = daos.deleteVoucher(code)

    suspend fun insertConfig(config: ConfigEntity) = daos.insertConfig(config)
    suspend fun insertNotification(notif: NotificationEntity) = daos.insertNotification(notif)

    // Auto seed data on first run
    suspend fun checkAndSeedInitialData() {
        val users = allUsers.firstOrNull()
        if (users == null || users.isEmpty()) {
            seedData()
        }
    }

    private suspend fun seedData() {
        // 1. Core Users
        val defaultCustomer = UserEntity(
            id = "cust_ahmad",
            role = "CUSTOMER",
            name = "Ahmad Muhiban",
            email = "amuhiban022@gmail.com",
            phone = "08123456789",
            profileImageUrl = "cust_avatar",
            balance = 500000.0,
            referralCode = "AHMAD022",
            status = "APPROVED"
        )
        daos.insertUser(defaultCustomer)

        val defaultAdmin = UserEntity(
            id = "admin_ utama",
            role = "ADMIN",
            name = "Admin PijatKu",
            email = "admin@pijatku.com",
            phone = "08111111222",
            status = "APPROVED"
        )
        daos.insertUser(defaultAdmin)

        // 2. Therapists
        val t1 = UserEntity(
            id = "therapist_budi",
            role = "THERAPIST",
            name = "Budi Santoso",
            email = "budi@pijatku.com",
            phone = "08129999888",
            profileImageUrl = "therapist_budi",
            isOnline = true,
            rating = 4.9f,
            totalReviews = 24,
            balance = 350000.0,
            customerCount = 14,
            ktpDoc = "KTP_BUDI.jpg",
            certDoc = "Sertifikat_Kesehatan_Budi.pdf",
            selfieDoc = "Selfie_Budi.jpg",
            workplaceDoc = "Homecare_Budi.jpg",
            status = "APPROVED"
        )
        val t2 = UserEntity(
            id = "therapist_ani",
            role = "THERAPIST",
            name = "Ani Lestari",
            email = "ani@pijatku.com",
            phone = "08127777666",
            profileImageUrl = "therapist_ani",
            isOnline = true,
            rating = 4.7f,
            totalReviews = 18,
            balance = 180000.0,
            customerCount = 9,
            ktpDoc = "KTP_ANI.jpg",
            certDoc = "Sertifikat_Refleksi_Ani.pdf",
            selfieDoc = "Selfie_Ani.jpg",
            workplaceDoc = "Homecare_Ani.jpg",
            status = "APPROVED"
        )
        val t3 = UserEntity(
            id = "therapist_eko",
            role = "THERAPIST",
            name = "Eko Prasetyo",
            email = "eko@pijatku.com",
            phone = "08125555444",
            profileImageUrl = "therapist_eko",
            isOnline = false,
            rating = 0.0f,
            totalReviews = 0,
            balance = 0.0,
            customerCount = 0,
            ktpDoc = "KTP_EKO.jpg",
            certDoc = "Sertifikat_Bekam_Eko.pdf",
            selfieDoc = "Selfie_Eko.jpg",
            workplaceDoc = "Workplace_Eko.jpg",
            status = "PENDING" // This one needs Admin Approval!
        )
        val t4 = UserEntity(
            id = "therapist_joko",
            role = "THERAPIST",
            name = "Joko Widodo",
            email = "joko@pijatku.com",
            phone = "08123333222",
            profileImageUrl = "therapist_joko",
            isOnline = false,
            rating = 4.2f,
            totalReviews = 4,
            balance = 45000.0,
            customerCount = 3,
            ktpDoc = "KTP_JOKO.jpg",
            certDoc = "Sertifikat_Standard_Joko.pdf",
            selfieDoc = "Selfie_Joko.jpg",
            workplaceDoc = "No_Workplace.jpg",
            status = "SUSPENDED" // This one is suspended
        )
        daos.insertUser(t1)
        daos.insertUser(t2)
        daos.insertUser(t3)
        daos.insertUser(t4)

        // 3. Vouchers
        daos.insertVoucher(VoucherEntity("MINTRELAX", 25000.0, 0, "Diskon pijat Rp 25.000 untuk pengguna baru", "VOUCHER"))
        daos.insertVoucher(VoucherEntity("PIJATSEHAT", 15000.0, 10, "Potongan Rp 15.000 + Cashback 10% koin", "CASHBACK"))
        daos.insertVoucher(VoucherEntity("RELAXSERU", 50000.0, 0, "Promo Eksklusif relaksasi akhir pekan", "VOUCHER"))

        // 4. Admin Config
        daos.insertConfig(ConfigEntity())

        // 5. Initial Notifications
        daos.insertNotification(NotificationEntity(
            title = "Selamat Datang!",
            description = "Nikmati diskon Rp 25.000 dengan kode voucher MINTRELAX.",
            relativeTo = "CUSTOMER",
            relativeId = "cust_ahmad"
        ))
    }
}
