package com.music.dhvani.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles incoming push notifications from Firebase Cloud Messaging (FCM).
 *
 * Automatically displays a rich update notification whenever a new release is published
 * on GitHub, allowing the user to update the app even if it hasn't been opened for days.
 */
class DhvaniFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM registration token received")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val version = data["version"]?.removePrefix("v")?.trim()
            ?: remoteMessage.notification?.title?.substringAfter("v")?.substringBefore(" ")?.trim()

        val releaseUrl = data["release_url"] ?: "https://github.com/Kanaiya-rgb/Dhvani-Music/releases"
        val apkUrl = data["apk_url"]
        val notes = data["notes"] ?: remoteMessage.notification?.body

        if (!version.isNullOrBlank()) {
            val updateInfo = AppUpdateChecker.UpdateInfo(
                version = version,
                releaseUrl = releaseUrl,
                apkUrl = apkUrl,
                notes = notes,
            )
            AppUpdateChecker.postUpdateNotification(applicationContext, updateInfo)
        } else {
            // Trigger background verification check against latest GitHub release
            CoroutineScope(Dispatchers.IO).launch {
                AppUpdateChecker.check(applicationContext)
            }
        }
    }

    companion object {
        private const val TAG = "DhvaniFCM"
    }
}
