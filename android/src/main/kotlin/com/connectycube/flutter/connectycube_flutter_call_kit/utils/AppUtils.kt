package com.connectycube.flutter.connectycube_flutter_call_kit.utils

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.util.Log

/**
 * Identify if the application is currently in a state where user interaction is possible. This
 * method is called when a remote message is received to determine how the incoming message should
 * be handled.
 *
 * @param context context.
 * @return True if the application is currently in a state where user interaction is possible,
 * false otherwise.
 */
fun isApplicationForeground(context: Context): Boolean {
    var keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE)
    if (keyguardManager == null) {
        return false
    } else {
        keyguardManager = keyguardManager as KeyguardManager
    }

    if (keyguardManager.isKeyguardLocked) {
        return false
    }

    var activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
    if (activityManager == null) {
        return false
    } else {
        activityManager = activityManager as ActivityManager
    }


    val appProcesses = activityManager.runningAppProcesses ?: return false
    val packageName = context.packageName
    // *  A workaround to fix the terminated state callbacks not firing in terminated state.
    // For more details check this issue:
    // https://github.com/ConnectyCube/connectycube-flutter-call-kit/issues/48
    Log.i("Kortobaa", "Current Version SDK  ${android.os.Build.VERSION.SDK_INT}")

    if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P){
        Log.i("Kortobaa", "Version <= API 28")
        return false
    }
    // * ============================= Remove when you find better solution !
    for (appProcess in appProcesses) {
        if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            && appProcess.processName == packageName
        ) {
            return true
        }
    }
    return false
}