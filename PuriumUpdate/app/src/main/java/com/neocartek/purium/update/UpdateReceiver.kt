package com.neocartek.purium.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UpdateReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
//        Log.e("UpdateReceiver action=", intent.action)
//        when (intent.action) {
//            Intent.ACTION_PACKAGE_REPLACED -> {
//                if (intent.data?.toString() == "package:" + "com.neocartek.purium.update"){
//                    val runtime = Runtime.getRuntime()
//                    try {
//                        val cmd = "reboot"
//                        runtime.exec(cmd)
//                    } catch (e: Exception) {
//                        e.fillInStackTrace()
//                    }
//                }
//            }
//        }
    }
}