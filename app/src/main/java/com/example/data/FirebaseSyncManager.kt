package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private var isInitialized = false
    private var database: FirebaseDatabase? = null
    private var firestore: FirebaseFirestore? = null

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
            firestore = FirebaseFirestore.getInstance()
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
        syncUserToFirestore(user)
    }

    fun syncUserToFirestore(user: UserEntity) {
        val fs = firestore ?: return
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
        fs.collection("users").document(user.id)
            .set(map)
            .addOnSuccessListener {
                Log.d(TAG, "User successfully synced to Firestore: ${user.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing user to Firestore: ${e.message}")
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
        syncOrderToFirestore(order)
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

    fun syncOrderToFirestore(order: OrderEntity) {
        val fs = firestore ?: return
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
        fs.collection("orders").document(order.id.toString())
            .set(map)
            .addOnSuccessListener {
                Log.d(TAG, "Order successfully synced to Firestore: ${order.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing order to Firestore: ${e.message}")
            }
    }

    fun fetchOrdersFromFirestore(
        customerId: String,
        onSuccess: (List<OrderEntity>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fs = firestore ?: run {
            onFailure(Exception("Firestore not initialized"))
            return
        }
        fs.collection("orders")
            .whereEqualTo("customerId", customerId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val orders = mutableListOf<OrderEntity>()
                for (doc in querySnapshot.documents) {
                    try {
                        val id = (doc.get("id") as? Long)?.toInt() ?: doc.id.toIntOrNull() ?: 0
                        val customerName = doc.getString("customerName") ?: ""
                        val customerPhone = doc.getString("customerPhone") ?: ""
                        val therapistId = doc.getString("therapistId") ?: ""
                        val therapistName = doc.getString("therapistName") ?: ""
                        val serviceName = doc.getString("serviceName") ?: ""
                        val price = (doc.get("price") as? Number)?.toDouble() ?: 0.0
                        val date = doc.getString("date") ?: ""
                        val time = doc.getString("time") ?: ""
                        val status = doc.getString("status") ?: "PENDING"
                        val paymentMethod = doc.getString("paymentMethod") ?: ""
                        val paymentStatus = doc.getString("paymentStatus") ?: ""
                        val address = doc.getString("address") ?: ""
                        val latitude = (doc.get("latitude") as? Number)?.toDouble() ?: 0.0
                        val longitude = (doc.get("longitude") as? Number)?.toDouble() ?: 0.0
                        val rating = (doc.get("rating") as? Long)?.toInt() ?: 0
                        val reviewComment = doc.getString("reviewComment") ?: ""
                        val timestamp = (doc.get("timestamp") as? Long) ?: System.currentTimeMillis()

                        orders.add(
                            OrderEntity(
                                id = id,
                                customerId = customerId,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                therapistId = therapistId,
                                therapistName = therapistName,
                                serviceName = serviceName,
                                price = price,
                                date = date,
                                time = time,
                                status = status,
                                paymentMethod = paymentMethod,
                                paymentStatus = paymentStatus,
                                address = address,
                                latitude = latitude,
                                longitude = longitude,
                                rating = rating,
                                reviewComment = reviewComment,
                                timestamp = timestamp
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing order document: ${e.message}")
                    }
                }
                onSuccess(orders.sortedByDescending { it.timestamp })
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun submitReviewToFirestore(
        orderId: Int,
        customerId: String,
        customerName: String,
        therapistId: String,
        rating: Int,
        comment: String,
        onComplete: () -> Unit
    ) {
        val fs = firestore ?: run {
            onComplete()
            return
        }
        val reviewId = "review_${orderId}"
        val map = mapOf(
            "id" to reviewId,
            "orderId" to orderId,
            "customerId" to customerId,
            "customerName" to customerName,
            "therapistId" to therapistId,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to System.currentTimeMillis()
        )
        fs.collection("reviews").document(reviewId)
            .set(map)
            .addOnSuccessListener {
                Log.d(TAG, "Review successfully stored in Firestore")
                recalculateTherapistRatingFromFirestore(therapistId, onComplete)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error storing review in Firestore: ${e.message}")
                onComplete()
            }
    }

    fun recalculateTherapistRatingFromFirestore(therapistId: String, onComplete: () -> Unit) {
        val fs = firestore ?: run {
            onComplete()
            return
        }
        fs.collection("reviews")
            .whereEqualTo("therapistId", therapistId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val count = querySnapshot.size()
                if (count > 0) {
                    var sum = 0.0
                    for (doc in querySnapshot.documents) {
                        sum += (doc.get("rating") as? Number)?.toDouble() ?: 0.0
                    }
                    val averageRating = (sum / count).toFloat()
                    
                    fs.collection("users").document(therapistId)
                        .update(
                            "rating", averageRating,
                            "totalReviews", count
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "Therapist ranking updated in Firestore: $averageRating")
                            onComplete()
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Failed to update therapist ranking in Firestore")
                            onComplete()
                        }
                } else {
                    onComplete()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to query reviews: ${e.message}")
                onComplete()
            }
    }

    fun fetchTherapistsFromFirestore(onSuccess: (List<UserEntity>) -> Unit) {
        val fs = firestore ?: return
        fs.collection("users")
            .whereEqualTo("role", "THERAPIST")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = mutableListOf<UserEntity>()
                for (doc in querySnapshot.documents) {
                    try {
                        val id = doc.getString("id") ?: continue
                        val rating = (doc.get("rating") as? Number)?.toFloat() ?: 4.8f
                        val totalReviews = (doc.get("totalReviews") as? Long)?.toInt() ?: 12
                        list.add(
                            UserEntity(
                                id = id,
                                role = "THERAPIST",
                                name = doc.getString("name") ?: "",
                                email = doc.getString("email") ?: "",
                                phone = doc.getString("phone") ?: "",
                                rating = rating,
                                totalReviews = totalReviews
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping therapist from Firestore: ${e.message}")
                    }
                }
                onSuccess(list)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch therapists from Firestore: ${e.message}")
            }
    }
}
