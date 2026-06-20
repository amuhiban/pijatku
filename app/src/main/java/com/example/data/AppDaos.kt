package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDaos {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserSync(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'THERAPIST'")
    fun getAllTherapists(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isOnline = :isOnline WHERE id = :id")
    suspend fun updateTherapistOnlineStatus(id: String, isOnline: Boolean)

    @Query("UPDATE users SET balance = :newBalance WHERE id = :id")
    suspend fun updateUserBalance(id: String, newBalance: Double)

    @Query("UPDATE users SET name = :name, email = :email, phone = :phone, profileImageUrl = :profileUrl WHERE id = :id")
    suspend fun updateUserProfile(id: String, name: String, email: String, phone: String, profileUrl: String)

    @Query("UPDATE users SET status = :status WHERE id = :id")
    suspend fun updateTherapistStatus(id: String, status: String)

    // --- Orders ---
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getOrdersForCustomer(customerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE therapistId = :therapistId ORDER BY timestamp DESC")
    fun getOrdersForTherapist(therapistId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderById(id: Int): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderSync(id: Int): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun updateOrderStatus(id: Int, status: String)

    @Query("UPDATE orders SET rating = :rating, reviewComment = :comment WHERE id = :id")
    suspend fun rateOrder(id: Int, rating: Int, comment: String)

    @Query("UPDATE orders SET paymentStatus = :paymentStatus WHERE id = :id")
    suspend fun updateOrderPaymentStatus(id: Int, paymentStatus: String)

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages WHERE orderId = :orderId ORDER BY timestamp ASC")
    fun getChatMessages(orderId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    // --- Vouchers ---
    @Query("SELECT * FROM vouchers")
    fun getAllVouchers(): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE code = :code")
    suspend fun getVoucherSync(code: String): VoucherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Query("DELETE FROM vouchers WHERE code = :code")
    suspend fun deleteVoucher(code: String)

    // --- Admin Config ---
    @Query("SELECT * FROM admin_config WHERE id = 'ADMIN_CONFIG'")
    fun getConfig(): Flow<ConfigEntity?>

    @Query("SELECT * FROM admin_config WHERE id = 'ADMIN_CONFIG'")
    suspend fun getConfigSync(): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity)

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE relativeId = :userId ORDER BY timestamp DESC")
    fun getNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notif: NotificationEntity)
}
