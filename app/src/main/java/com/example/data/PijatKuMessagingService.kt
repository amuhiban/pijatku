package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore

class PijatKuMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM Token: $token")
        
        // Save token to Firestore if current user exists
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            updateTokenInFirestore(currentUserId, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            val title = remoteMessage.data["title"] ?: "PijatKu Update"
            val body = remoteMessage.data["body"] ?: "Ada update terbaru mengenai pesanan Anda."
            sendNotification(title, body)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val title = it.title ?: "PijatKu Update"
            val body = it.body ?: "Ada update terbaru mengenai pesanan Anda."
            sendNotification(title, body)
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "pijatku_status_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(applicationInfo.icon) // Use default application icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Status Pesanan & Terapis",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi real-time update perjalanan terapis dan perubahan status pesanan."
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun getCurrentUserId(): String? {
        // Simple fallback helper to find the currently active user id
        val sharedPrefs = getSharedPreferences("pijatku_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("current_user_id", null)
    }

    companion object {
        private const val TAG = "PijatKuMessagingService"
        
        fun updateTokenInFirestore(userId: String, token: String) {
            try {
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully updated user token in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating user fcmToken in Firestore: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore update exception for FCM Token: ${e.message}")
            }
        }
    }
}
