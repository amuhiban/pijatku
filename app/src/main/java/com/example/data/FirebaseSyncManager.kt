package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private var isInitialized = false
    private var database: FirebaseDatabase? = null

    // Initialize Firebase programmatically using custom credentials
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyBGA4b7l9AYL3zzqoF1T3BalMy1FW-9VoU")
                .setApplicationId("1:737170434650:web:408cd51130ab9dca388663")
                .setDatabaseUrl("https://sniper-gold-mlm-default-rtdb.asia-southeast1.firebasedatabase.app")
                .setProjectId("sniper-gold-mlm")
                .setStorageBucket("sniper-gold-mlm.firebasestorage.app")
                .setGcmSenderId("737170434650")
                .build()

            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "Firebase initialized dynamically with custom credentials.")
            }
            
            database = FirebaseDatabase.getInstance()
            // Enable offline persistence on Firebase to sync safely when offline
            try {
                database?.setPersistenceEnabled(true)
            } catch (e: Exception) {
                // Persistence can only be set once at startup
                Log.w(TAG, "Persistence already configured or error: ${e.message}")
            }
            
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
        }
    }

    private fun getRef(path: String): DatabaseReference? {
        val db = database ?: return null
        return db.getReference(path)
    }

    // --- Dynamic Sync Functions ---

    fun syncUser(user: UserEntity) {
        val map = mapOf(
            "id" to user.id,
            "role" to user.role,
            "name" to user.name,
            "email" to user.email,
            "phone" to user.phone,
            "profileImageUrl" to user.profileImageUrl,
            "isOnline" to user.isOnline,
            "rating" to user.rating,
            "totalReviews" to user.totalReviews,
            "balance" to user.balance,
            "customerCount" to user.customerCount,
            "ktpDoc" to user.ktpDoc,
            "certDoc" to user.certDoc,
            "selfieDoc" to user.selfieDoc,
            "workplaceDoc" to user.workplaceDoc,
            "status" to user.status,
            "referralCode" to user.referralCode,
            "referredBy" to user.referredBy
        )
        getRef("pijatku/users/${user.id}")?.setValue(map)?.addOnFailureListener {
            Log.e(TAG, "Failed to sync user: ${it.message}")
        }
    }

    fun syncOrder(order: OrderEntity) {
        val map = mapOf(
            "id" to order.id,
            "customerId" to order.customerId,
            "customerName" to order.customerName,
            "customerPhone" to order.customerPhone,
            "therapistId" to order.therapistId,
            "therapistName" to order.therapistName,
            "serviceName" to order.serviceName,
            "price" to order.price,
            "date" to order.date,
            "time" to order.time,
            "status" to order.status,
            "paymentMethod" to order.paymentMethod,
            "paymentStatus" to order.paymentStatus,
            "address" to order.address,
            "latitude" to order.latitude,
            "longitude" to order.longitude,
            "rating" to order.rating,
            "reviewComment" to order.reviewComment,
            "timestamp" to order.timestamp
        )
        getRef("pijatku/orders/${order.id}")?.setValue(map)?.addOnFailureListener {
            Log.e(TAG, "Failed to sync order: ${it.message}")
        }
    }

    fun syncChatMessage(message: ChatMessageEntity) {
        val map = mapOf(
            "id" to message.id,
            "orderId" to message.orderId,
            "senderId" to message.senderId,
            "receiverId" to message.receiverId,
            "message" to message.message,
            "timestamp" to message.timestamp
        )
        getRef("pijatku/chats/${message.orderId}/${message.id}")?.setValue(map)?.addOnFailureListener {
            Log.e(TAG, "Failed to sync chat message: ${it.message}")
        }
    }

    fun syncVoucher(voucher: VoucherEntity) {
        val map = mapOf(
            "code" to voucher.code,
            "discountAmount" to voucher.discountAmount,
            "cashbackPercent" to voucher.cashbackPercent,
            "description" to voucher.description,
            "type" to voucher.type
        )
        getRef("pijatku/vouchers/${voucher.code}")?.setValue(map)?.addOnFailureListener {
            Log.e(TAG, "Failed to sync voucher: ${it.message}")
        }
    }

    fun deleteVoucher(code: String) {
        getRef("pijatku/vouchers/$code")?.removeValue()?.addOnFailureListener {
            Log.e(TAG, "Failed to delete voucher: ${it.message}")
        }
    }

    fun syncConfig(config: ConfigEntity) {
        val map = mapOf(
            "id" to config.id,
            "platformCommissionPercent" to config.platformCommissionPercent,
            "therapistSharePercent" to config.therapistSharePercent,
            "serviceFee" to config.serviceFee
        )
        getRef("pijatku/config/${config.id}")?.setValue(map)?.addOnFailureListener {
            Log.e(TAG, "Failed to sync config: ${it.message}")
        }
    }

    fun syncNotification(notif: NotificationEntity) {
        val map = mapOf(
            "id" to notif.id,
            "title" to notif.title,
            "description" to notif.description,
            "relativeTo" to notif.relativeTo,
            "relativeId" to notif.relativeId,
            "timestamp" to notif.timestamp
        )
        getRef("pijatku/notifications/${notif.id}")?.setValue(map)?.addOnFailureListener {
            Log.e(TAG, "Failed to sync notification: ${it.message}")
        }
    }
}
