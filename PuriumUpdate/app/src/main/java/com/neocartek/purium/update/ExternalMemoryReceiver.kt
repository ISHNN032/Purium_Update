package com.neocartek.purium.update

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK



class ExternalMemoryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("ExternalMemoryReceiver action=", intent.action)

        when (intent.action) {
            Intent.ACTION_MEDIA_UNMOUNTED -> {
                Commander.closeSerialClient()
            }
        }
    }
}