package io.nekohasekai.sagernet.vpn.utils

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.nekohasekai.sagernet.vpn.repositories.AppRepository

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if the message contains a notification payload.
        remoteMessage.notification?.let {
            AppRepository.debugLog("Message Notification Body: ${it.body}")
            // You can handle the notification here, e.g., show it in a notification bar.
        }
        remoteMessage.data.isNotEmpty().let {
            AppRepository.debugLog("Message data payload: " + remoteMessage.data)
            // Handle the data payload of the FCM message.
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AppRepository.debugLog("Refreshed token: $token")
        // If you want to send messages to this application instance or manage this app server's subscriptions, send the FCM token to your server.
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}