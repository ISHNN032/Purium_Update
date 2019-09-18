package com.neocartek.purium.update

import android.app.AlertDialog
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.RecoverySystem
import android.os.SystemClock
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_button_os.isEnabled = false
        main_button_st.isEnabled = false

        var path = intent.getStringExtra("Path")
        val types = intent.getIntArrayExtra("Files")

        //var path = "/storage/udiskh"
        //var types = intArrayOf(1)

        Log.e("Path Extra", path)

        if (types != null)
            for (i in types) {
                Log.e("Files Extra", "" + i)
                update(i, path)
            }
    }


    fun update(type: Int, path: String) {
        when (type) {
            Constants.PREF_VALUE_ST -> {
                main_button_st.isEnabled = true
                main_button_st.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("ST MCU Update")
                        .setMessage("ST MCU 업데이트를 진행합니다.")
                        .setPositiveButton(android.R.string.ok) { _,_->
                            val fUpdate = File(path + File.separator + Constants.FILE_NAME_MCU_ST)

                            if (fUpdate.isFile) {
                                UpdateST(this, fUpdate.absolutePath)
                            }
                        }
                        .setNegativeButton(
                            android.R.string.cancel
                        ) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                return
            }
            Constants.PREF_VALUE_OTA -> {
                main_button_os.isEnabled = true
                main_button_os.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("OTA Update")
                        .setMessage("OTA Update 를 진행합니다.")
                        .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { _, _ ->

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
                            return@OnClickListener
                        })
                        .setNegativeButton(
                            android.R.string.cancel
                        ) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                return
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
            if(len > 0){
                output.write(buf, 0, len)
            }
            else
                break
        }
        input.close()
        output.close()
    }
}
