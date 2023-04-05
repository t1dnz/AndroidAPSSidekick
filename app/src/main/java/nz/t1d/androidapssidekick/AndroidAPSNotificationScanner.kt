package nz.t1d.AndroidAPSSidekick

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import androidx.localbroadcastmanager.content.LocalBroadcastManager


/*
This class captures the AndroidAPS FX notifications as RemoteView and then sends them to the main activity to be processed
 */
class AndroidAPSNotificationScanner : NotificationListenerService() {
    private val packagesToListenTo = hashSetOf(
        "com.dexcom.g7",
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName: String = sbn.packageName
        val notification: Notification = sbn.notification
        val ongoing: Boolean = sbn.isOngoing
        val contentView = notification.contentView

        if (!packagesToListenTo.contains(packageName) || !ongoing || contentView == null) {
            return
        }

        sendMessageToActivity(contentView)
    }

    private fun sendMessageToActivity(rv: RemoteViews) {
        val intent = Intent("AndroidAPSFXNotification")
        // You can also include some extra data.
        intent.putExtra("view", rv)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        //
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}

