package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // e.g. "cust_1", "therapist_1", "admin_1"
    val role: String,           // "CUSTOMER", "THERAPIST", "ADMIN"
    val name: String,
    val email: String,
    val phone: String,
    val profileImageUrl: String = "",
    val isOnline: Boolean = false,
    val rating: Float = 4.8f,
    val totalReviews: Int = 12,
    val balance: Double = 150000.0,
    val customerCount: Int = 8,
    val ktpDoc: String = "",
    val certDoc: String = "",
    val selfieDoc: String = "",
    val workplaceDoc: String = "",
    val status: String = "APPROVED", // "PENDING", "APPROVED", "SUSPENDED"
    val referralCode: String = "",
    val referredBy: String = ""
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val therapistId: String,
    val therapistName: String,
    val serviceName: String,
    val price: Double,
    val date: String,
    val time: String,
    val status: String,    // "MENUNGGU", "MENUJU_LOKASI", "TIBA", "MELAYANI", "SELESAI", "BATAL"
    val paymentMethod: String, // "QRIS", "TRANSFER", "EWALLET", "CASH"
    val paymentStatus: String, // "PENDING", "SUCCESS"
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Int = 0, // 0 means unrated
    val reviewComment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val senderId: String,
    val receiverId: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey val code: String,
    val discountAmount: Double,
    val cashbackPercent: Int = 0,
    val description: String,
    val type: String // "VOUCHER", "CASHBACK"
)

@Entity(tableName = "admin_config")
data class ConfigEntity(
    @PrimaryKey val id: String = "ADMIN_CONFIG",
    val platformCommissionPercent: Float = 20.0f,
    val therapistSharePercent: Float = 80.0f,
    val serviceFee: Double = 5000.0
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val relativeTo: String, // "CUSTOMER", "THERAPIST"
    val relativeId: String, // customerId or therapistId
    val timestamp: Long = System.currentTimeMillis()
)
