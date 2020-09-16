package com.neocartek.purium.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.neocartek.purium.update.Constants.MSG_UPDATE_MANAGER_SUCCEED
import com.neocartek.purium.update.Constants.MSG_UPDATE_MEDIA_SUCCEED
import com.neocartek.purium.update.Constants.MSG_UPDATE_PURIUM_SUCCEED
import com.neocartek.purium.update.Constants.PACKAGE_NAME_MANAGER
import com.neocartek.purium.update.Constants.PACKAGE_NAME_MEDIA
import com.neocartek.purium.update.Constants.PACKAGE_NAME_PURIUM
import com.neocartek.purium.update.Constants.PACKAGE_NAME_UPDATE
import com.neocartek.purium.update.update.UpdateFragment

class UpdateReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        Log.e("UpdateReceiver action=", intent.action)
        when (intent.action) {
            Intent.ACTION_PACKAGE_REPLACED -> {
                when(intent.data?.toString()){
                    "package:$PACKAGE_NAME_MANAGER"->{
                        UpdateFragment.instance?.updateHandler?.sendEmptyMessage(
                            MSG_UPDATE_MANAGER_SUCCEED)
                        Log.e("APPUpdate", "MANAGER updated!")
                        Toast.makeText(context, "Manager app updated.", Toast.LENGTH_SHORT).show()
                    }
                    "package:$PACKAGE_NAME_PURIUM"->{
                        UpdateFragment.instance?.updateHandler?.sendEmptyMessage(
                            MSG_UPDATE_PURIUM_SUCCEED)
                        Log.e("APPUpdate", "PURIUM updated!")
                        Toast.makeText(context, "Purium app updated.", Toast.LENGTH_SHORT).show()
                    }
                    "package:$PACKAGE_NAME_MEDIA"->{
                        UpdateFragment.instance?.updateHandler?.sendEmptyMessage(
                            MSG_UPDATE_MEDIA_SUCCEED)
                        Log.e("APPUpdate", "MEDIA updated!")
                        Toast.makeText(context, "Media app updated.", Toast.LENGTH_SHORT).show()
                    }
                    "package:$PACKAGE_NAME_UPDATE"->{
//                        UpdateFragment.instance?.updateHandler?.sendEmptyMessage(
//                            MSG_UPDATE_UPDATE_SUCCEED)
                        Log.e("APPUpdate", "UPDATE updated!")
                        val i = Intent()
                        i.setClassName(
                            "com.neocartek.purium.update",
                            "com.neocartek.purium.update.MainActivity"
                        )
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(i)

                        Thread.sleep(3000)
                        Toast.makeText(context, "Update app updated.", Toast.LENGTH_SHORT).show()
                        Thread.sleep(2000)
                        val runtime = Runtime.getRuntime()
                        try {
                            val cmd = "reboot"
                            runtime.exec(cmd)
                        } catch (e: Exception) {
                            e.fillInStackTrace()
                        }
                    }
                }
            }
        }
    }
}