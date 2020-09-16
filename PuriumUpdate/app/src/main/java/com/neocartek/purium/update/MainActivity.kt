package com.neocartek.purium.update

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.neocartek.purium.update.update.UpdateFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val runtime = Runtime.getRuntime()
        try {
            var cmd = "am force-stop com.neocartek.purium.manager"
            Log.e("STOP", "Purium_Manager")
            var process = runtime.exec(cmd)
            process.errorStream.close()
            process.inputStream.close()
            process.outputStream.close()
        } catch (e: Exception) {
            e.fillInStackTrace()
        }

        main_button_os.isEnabled = false
        main_button_st.isEnabled = false


        var path = intent.getStringExtra("Path")
        val types = intent.getIntArrayExtra("Files")

        //var path = "/storage/udiskh"
        //var types = intArrayOf(1)

        if(path != null) {
            Log.e("Path Extra", path)
        }

        if (types != null) {
            for (i in types) {
                Log.e("Files Extra", "" + i)
                update(i, path)
            }
        }
    }


    fun update(type: Int, path: String) {
        when (type) {
            Constants.PREF_VALUE_ST -> {
                val file = File(Commander.update_path + File.separator + Constants.FILE_NAME_MCU_ST)
                main_text_st_new.text = Date(file.lastModified()).toString()
                main_button_st.setTextColor(Color.WHITE)

                main_button_st.isEnabled = true
                main_button_st.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("ST MCU Update")
                        .setMessage("ST MCU Update will be Started.")
                        .setPositiveButton(android.R.string.ok) { _,_->
                            Commander.update_path = path
                            Commander.setSerialClient(this)

                            val fUpdate = File(path + File.separator + Constants.FILE_NAME_MCU_ST)
                            if (fUpdate.isFile) {
                                Commander.update_Type = Constants.PREF_VALUE_ST
                                Commander.update_File = fUpdate
                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.update_fragment, UpdateFragment())
                                    .commit()

                                main_button_os.isEnabled = false
                                main_button_st.isEnabled = false
                                main_button_app.isEnabled = false
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
                val file = File(Commander.update_path + File.separator + Constants.FILE_NAME_CPU_OTA)
                main_text_os_new.text = Date(file.lastModified()).toString()
                main_button_os.setTextColor(Color.WHITE)

                main_button_os.isEnabled = true
                main_button_os.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("OTA Update")
                        .setMessage("OTA Update will be Started.")
                        .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { _, _ ->
                            SystemClock.sleep(2000)

                            val fUpdate = File(path + File.separator + Constants.FILE_NAME_CPU_OTA)
                            if (fUpdate.isFile) {
                                Commander.update_Type = Constants.PREF_VALUE_OTA
                                Commander.update_File = fUpdate
                                supportFragmentManager.beginTransaction()
                                    .replace(R.id.update_fragment, UpdateFragment())
                                    .commit()
                                main_button_os.isEnabled = false
                                main_button_st.isEnabled = false
                                main_button_app.isEnabled = false
                            }
                        })
                        .setNegativeButton(
                            android.R.string.cancel
                        ) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                return
            }
            Constants.PREF_VALUE_APP -> {
                main_button_st.setTextColor(Color.WHITE)
                main_button_app.isEnabled = true
                main_button_app.setOnClickListener {
                    Commander.update_path = path
                    Commander.update_Type = Constants.PREF_VALUE_APP
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.update_fragment, UpdateFragment())
                        .commit()
                    main_button_os.isEnabled = false
                    main_button_st.isEnabled = false
                    main_button_app.isEnabled = false
                }
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
