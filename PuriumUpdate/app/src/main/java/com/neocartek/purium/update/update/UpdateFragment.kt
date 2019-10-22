package com.neocartek.purium.update.update

import android.view.ContextMenu

import android.os.Bundle
import android.os.RecoverySystem
import android.support.v4.app.Fragment
import android.util.Log
import android.view.View
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import com.neocartek.purium.update.Commander
import com.neocartek.purium.update.Constants
import com.neocartek.purium.update.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*

class UpdateFragment : Fragment() {
    @Override
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return when (Commander.update_Type) {
            Constants.PREF_VALUE_ST -> {
                inflater.inflate(R.layout.fragment_update, container, false)
            }
            Constants.PREF_VALUE_OTA -> {
                inflater.inflate(R.layout.fragment_update_ota, container, false)
            }
            else -> {
                inflater.inflate(R.layout.fragment_update_none, container, false)
            }
        }
    }

    @Override
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runBlocking {
            when (Commander.update_Type) {
                Constants.PREF_VALUE_ST -> {
                    Log.e("update ST", "ST Update Start")
                    //update(Constants.PREF_VALUE_ST, Commander.update_File.absolutePath)
                }
                Constants.PREF_VALUE_OTA -> {
                    Log.e("update OTA", "OTA Update Start")
                    update(Constants.PREF_VALUE_OTA, Commander.update_File.absolutePath)
                }
                else -> {
                    //
                }
            }
        }
    }

    suspend fun update(type: Int, path: String) {
        when (type) {
            Constants.PREF_VALUE_ST -> {
                var job = GlobalScope.launch {
                    while(!Commander.update_ready) Thread.sleep(500)

                    UpdateST(context!!, Commander.update_File.absolutePath)
                }
                job.join()
            }

            Constants.PREF_VALUE_OTA -> {
                var job = GlobalScope.launch {
                    SystemClock.sleep(2000)
                    try {
                        copyFile(File(Commander.update_File.absolutePath), File("/cache/ota.zip"))
                        Log.d("update", "MainActivity copy file ${Commander.update_File.absolutePath} /cache/ota.zip")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    SystemClock.sleep(1000)
                    try {
                        RecoverySystem.installPackage(context, File("/cache/ota.zip"))
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                job.join()
            }
        }
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun copyFile(sourceLocation: File, targetLocation: File) {
        val input = FileInputStream(sourceLocation)
        val output = FileOutputStream(targetLocation)

        // Copy the bits from in stream to out stream
        val buf = ByteArray(1024)
        var len: Int
        while (true) {
            len = input.read(buf)
            if (len > 0) {
                output.write(buf, 0, len)
            } else
                break
        }
        input.close()
        output.close()
    }
}