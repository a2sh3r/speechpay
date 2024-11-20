package com.example.speechpay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle.getParcelableArray("pdus")
            if (pdus != null) {
                for (pdu in pdus) {
                    val message = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = message.displayOriginatingAddress
                    val body = message.displayMessageBody
                    if (sender == "900") {
                        val intentUpdate = Intent("com.example.speechpay.UPDATE_SMS")
                        intentUpdate.putExtra("sms_body", body)
                        context.sendBroadcast(intentUpdate)
                    }
                }
            }
        }
    }
}
