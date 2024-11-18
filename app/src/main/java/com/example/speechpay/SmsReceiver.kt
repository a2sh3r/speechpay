package com.example.speechpay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.widget.Toast

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>
                val messages = arrayOfNulls<SmsMessage>(pdus.size)

                for (i in pdus.indices) {
                    messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                }

                val message = messages[0]?.messageBody
                Toast.makeText(context, "SMS получено: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
