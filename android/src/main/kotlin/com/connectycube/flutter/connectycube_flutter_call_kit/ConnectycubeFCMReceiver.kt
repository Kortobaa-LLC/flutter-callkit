package com.connectycube.flutter.connectycube_flutter_call_kit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.ContextHolder
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val callingCallsMap = mutableMapOf<String, String>()
private val canceledCallsMap = mutableMapOf<String, String>()

class ConnectycubeFCMReceiver : BroadcastReceiver() {
    private val TAG = "ConnectycubeFCMReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Enigma broadcast received for message")

        ContextHolder.applicationContext = context!!.applicationContext

        if (intent!!.extras == null) {
            Log.d(
                TAG,
                "Enigma broadcast received but intent contained no extras to process RemoteMessage. Operation cancelled."
            )
            return
        }

        val remoteMessage = RemoteMessage(intent.extras!!)

        val data = remoteMessage.data
        val callId = data["uuid"] ?: return
        val processType = data["process_type"] ?: return
        when (processType) {
            "calling" -> {
                callingCallsMap[callId] = processType
            }
            "canceled" ->
            {
                canceledCallsMap[callId] = processType
            }
        }


        if (canceledCallsMap.containsKey(callId)) {
            println("Enigma Missed Call $callId")
            processEndCallEvent(context.applicationContext, data)
        }
        else if (callingCallsMap.containsKey(callId)) {
            println("Enigma New Call $callId")
            // Wait before check
            TimeUnit.MILLISECONDS.sleep(500)
            if (canceledCallsMap.containsKey(callId)) {
                println("Enigma Missed Call $callId")
                processEndCallEvent(context.applicationContext, data)
            }
            else {
                println("Enigma New Active Call $callId")
                processInviteCallEvent(context.applicationContext, data)

            }
        }

        println("Enigma Calling Map $callingCallsMap")
        println("Enigma Canceled Map $canceledCallsMap")
    }

    private fun processEndCallEvent(applicationContext: Context, data: Map<String, String>) {
        val callId = data["uuid"] ?: return


        processCallEnded(applicationContext, callId)
    }

    private fun processInviteCallEvent(applicationContext: Context, data: Map<String, String>) {
        val callId = data["uuid"]

        if (callId == null || CALL_STATE_UNKNOWN != getCallState(
                applicationContext,
                callId
            )
        ) {
            return
        }

        val callType =   2 // data["call_type"]?.toInt()  // => video : 1 / audio : 2
        val callInitiatorId = data["caller_id"]?.toInt()
        val callInitiatorName = data["caller_name"]
        val callOpponents =  arrayListOf(callInitiatorId ?: -1 )


        val meetingToken = data["token"]
        val userInfoData = JSONObject()
        userInfoData.put("meetingToken","$meetingToken")
        val userInfo =   userInfoData.toString()   // data["user_info"] ?: JSONObject(emptyMap<String, String>()).toString()

        if (callType == null || callInitiatorId == null || callInitiatorName == null || meetingToken == null) {
            return
        }

        showCallNotification(
            applicationContext,
            callId,
            callType,
            callInitiatorId,
            callInitiatorName,
            callOpponents,
            userInfo
        )

        saveCallState(applicationContext, callId, CALL_STATE_PENDING)
        saveCallData(applicationContext, callId, data)
        saveCallId(applicationContext, callId)
    }
}