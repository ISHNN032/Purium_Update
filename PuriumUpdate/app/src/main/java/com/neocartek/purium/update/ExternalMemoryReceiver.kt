package com.neocartek.purium.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.support.v4.app.ActivityCompat.finishAffinity
import kotlin.system.exitProcess


class ExternalMemoryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("ExternalMemoryReceiver action=", intent.action)
        when (intent.action) {
            Intent.ACTION_MEDIA_UNMOUNTED -> {
                exitProcess(-1)
            }
        }
    }
}