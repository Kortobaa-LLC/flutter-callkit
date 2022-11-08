package com.connectycube.flutter.connectycube_flutter_call_kit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getColorizedText
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getString

const val CALL_CHANNEL_ID = "calls_channel_id"
const val CALL_CHANNEL_NAME = "Calls"


fun cancelCallNotification(context: Context, callId: String) {
    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.cancel(callId.hashCode())
}

fun showCallNotification(
    context: Context, callId: String,  callType: Int, callInitiatorId: Int,
    callInitiatorName: String, callOpponents: ArrayList<Int>, userInfo: String , callToken:String, additionalData : HashMap<String,Any?>?
) {
    val notificationManager = NotificationManagerCompat.from(context)

    val intent = getLaunchIntent(context)

    val pendingIntent = PendingIntent.getActivity(
        context,
        callId.hashCode(),
        intent,
        getPendingIntentFlag()
    )

    var ringtone: Uri

    val customRingtone = getString(context, "ringtone")
    Log.d("NotificationsManager", "customRingtone $customRingtone")
    if (!TextUtils.isEmpty(customRingtone)) {
        ringtone = Uri.parse("android.resource://" + context.packageName + "/raw/" + customRingtone)
        Log.d("NotificationsManager", "ringtone 1 $ringtone")
    } else {
        ringtone = Settings.System.DEFAULT_RINGTONE_URI
    }

    Log.d("NotificationsManager", "ringtone 2 $ringtone")

    val callTypeTitle =
        String.format(CALL_TYPE_PLACEHOLDER, if (callType == 1) "Video" else "Audio")
    // *------ To create full screen calling intent
    val callFullScreenIntent: Intent = createStartIncomingScreenIntent(
        context,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo,
        callToken,
        additionalData
    )
    val fullScreenPendingIntent = PendingIntent.getActivity(
        context,
        callId.hashCode(),
        callFullScreenIntent,
        getPendingIntentFlag()
    )
    // *------ End of modification code !
    val builder: NotificationCompat.Builder =
        createCallNotification(context, callInitiatorName, callTypeTitle, pendingIntent,fullScreenPendingIntent ,ringtone)


    // TODO Add callToken and additionalData
    // Add actions
    addCallRejectAction(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo
    )
    addCallAcceptAction(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo
    )

    // Add full screen intent (to show on lock screen)
    addCallFullScreenIntent(
        builder,
        fullScreenPendingIntent
    )

    // Add action when delete call notification
    addCancelCallNotificationIntent(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        userInfo
    )

    // Set small icon for notification
    setNotificationSmallIcon(context, builder)

    // Set notification color accent
    setNotificationColor(context, builder)

    createCallNotificationChannel(notificationManager, ringtone)

    notificationManager.notify(callId.hashCode(), builder.build())
}

fun getLaunchIntent(context: Context): Intent? {
    val packageName = context.packageName
    val packageManager: PackageManager = context.packageManager
    return packageManager.getLaunchIntentForPackage(packageName)
}

fun createCallNotification(
    context: Context,
    title: String,
    text: String?,
    pendingIntent: PendingIntent,
    callFullScreenIntent: PendingIntent,
    ringtone: Uri
): NotificationCompat.Builder {
    val notificationBuilder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
    notificationBuilder
        .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        .setContentTitle(title)
        .setContentText(text)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(false)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setFullScreenIntent(callFullScreenIntent,true)
        .setContentIntent(pendingIntent)
        .setSound(ringtone)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setTimeoutAfter(60000)
    return notificationBuilder
}

fun addCallRejectAction(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    opponents: ArrayList<Int>,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, opponents)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val declinePendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        callId.hashCode(),
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_REJECT)
            .putExtras(bundle),
        getPendingIntentFlag()
    )
    val declineAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
        context.resources.getIdentifier(
            "ic_menu_close_clear_cancel",
            "drawable",
            context.packageName
        ),
        getColorizedText("Reject", "#E02B00"),
        declinePendingIntent
    )
        .build()

    notificationBuilder.addAction(declineAction)
}

fun addCallAcceptAction(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    opponents: ArrayList<Int>,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, opponents)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val acceptPendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        callId.hashCode(),
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_ACCEPT)
            .putExtras(bundle),
            getPendingIntentFlag()
    )
    val acceptAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
        context.resources.getIdentifier("ic_menu_call", "drawable", context.packageName),
        getColorizedText("Accept", "#4CB050"),
        acceptPendingIntent
    )
        .build()
    notificationBuilder.addAction(acceptAction)
}

fun addCallFullScreenIntent(
    notificationBuilder: NotificationCompat.Builder,
    fullScreenPendingIntent : PendingIntent
) {
    notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
}

fun addCancelCallNotificationIntent(
    appContext: Context?,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val deleteCallNotificationPendingIntent = PendingIntent.getBroadcast(
        appContext,
        callId.hashCode(),
        Intent(appContext, EventReceiver::class.java)
            .setAction(ACTION_CALL_NOTIFICATION_CANCELED)
            .putExtras(bundle),
        getPendingIntentFlag()
    )
    notificationBuilder.setDeleteIntent(deleteCallNotificationPendingIntent)
}

fun createCallNotificationChannel(notificationManager: NotificationManagerCompat, sound: Uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            CALL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(
            sound, AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
        )
        notificationManager.createNotificationChannel(channel)
    }
}

fun setNotificationSmallIcon(context: Context, notificationBuilder: NotificationCompat.Builder) {
    val customIcon = getString(context, "notification_icon")

    val resID =
    context.resources.getIdentifier(customIcon ?: "ic_launcher_foreground", "drawable", context.packageName)
    if (resID != 0) {
        notificationBuilder.setSmallIcon(resID)
    } else {
        notificationBuilder.setSmallIcon(context.applicationInfo.icon)
    }
}

fun setNotificationColor(context: Context, notificationBuilder: NotificationCompat.Builder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val color = getString(context, "color")

        if (!TextUtils.isEmpty(color)) {
            notificationBuilder.color = Color.parseColor(color)
        } else {
            val accentID = context.resources.getIdentifier(
                "call_notification_color_accent",
                "color",
                context.packageName
            )
            if (accentID != 0) {
                notificationBuilder.color = context.resources.getColor(accentID, null)
            } else {
                notificationBuilder.color = Color.parseColor("#4CAF50")
            }
        }
    }
}


/// A function for getting the approbriate PendingIntent Flag depending on the SDK API version.
fun getPendingIntentFlag():Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT

