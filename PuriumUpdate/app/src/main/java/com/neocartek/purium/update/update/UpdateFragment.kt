package com.neocartek.purium.update.update

/*

import android.app.ProgressDialog
import android.view.ContextMenu

import android.os.Bundle
import android.os.RecoverySystem
import android.support.v4.app.Fragment
import android.util.Log
import android.view.View
import android.os.SystemClock
import android.view.WindowManager
import android.widget.ProgressBar
import com.neocartek.purium.update.Constants
import com.neocartek.purium.update.MainActivity

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class UpdateFragment : Fragment() {
    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            internal var count = 5

            override fun run() {
                if (count < 0) {
                    when (1/*todo type*/) {
                        Constants.PREF_VALUE_ST -> {
                            val t = object : Thread() {
                                override fun run() {
                                    SystemClock.sleep(2000)
                                    activity!!.runOnUiThread {
                                        MainActivity.mActivity.mUpdateMCU =
                                            UpdateST(context!!, MainActivity.mActivity.fUpdate.getAbsolutePath())
                                    }
                                }

                            }
                            t.start()
                        }

                        Constants.PREF_VALUE_OTA -> {
                            val t = object : Thread() {
                                override fun run() {
                                    SystemClock.sleep(2000)

                                    val fUpdate = File(path + File.separator + Constants.FILE_NAME_CPU_OTA)

                                    try {
                                        copyFile(File(fUpdate.absolutePath), File("/cache/ota.zip"))
                                        Log.d("update","MainActivity copy file ${fUpdate.absolutePath} /cache/ota.zip")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    SystemClock.sleep(1000)
                                    try {
                                        RecoverySystem.installPackage(this@MainActivity, File("/cache/ota.zip"))
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            t.start()
                        }
                    }
                    timer.cancel()
                    timer.purge()
                    return
                } else {
                    activity!!.runOnUiThread { countText.text = "" + count-- }
                }
            }
        }, 0, 1000)
    }
}

*/