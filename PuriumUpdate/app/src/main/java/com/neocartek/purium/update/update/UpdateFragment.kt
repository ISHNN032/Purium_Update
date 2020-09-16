package com.neocartek.purium.update.update

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.RecoverySystem
import android.os.SystemClock
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.view.ContextThemeWrapper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.neocartek.purium.update.Commander
import com.neocartek.purium.update.Constants
import com.neocartek.purium.update.Constants.MSG_UPDATE_MANAGER_SUCCEED
import com.neocartek.purium.update.Constants.MSG_UPDATE_MEDIA_SUCCEED
import com.neocartek.purium.update.Constants.MSG_UPDATE_PURIUM_SUCCEED
import com.neocartek.purium.update.Constants.MSG_UPDATE_UPDATE_SUCCEED
import com.neocartek.purium.update.R
import com.neocartek.purium.update.update_intro.Command
import com.neocartek.purium.update.update_intro.ST_MCU_0
import com.neocartek.purium.update.update_intro.ST_MCU_1
import kotlinx.android.synthetic.main.fragment_update.*
import kotlinx.android.synthetic.main.fragment_update.progressBar_ttyS4
import kotlinx.android.synthetic.main.fragment_update_app.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*


class UpdateFragment : Fragment() {
    companion object {
        internal var instance: UpdateFragment? = null
    }

    init {
        instance = this
    }

    @Override
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return when (Commander.update_Type) {
            Constants.PREF_VALUE_ST -> {
                inflater.inflate(R.layout.fragment_update, container, false)
            }
            Constants.PREF_VALUE_OTA -> {
                inflater.inflate(R.layout.fragment_update_ota, container, false)
            }
            Constants.PREF_VALUE_APP -> {
                inflater.inflate(R.layout.fragment_update_app, container, false)
            }
            else -> {
                inflater.inflate(R.layout.fragment_update_none, container, false)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        runBlocking {
            when (Commander.update_Type) {
                Constants.PREF_VALUE_ST -> {
                    Log.e("update ST", "ST Update Start")
                    update(Constants.PREF_VALUE_ST, ST_MCU_0)
                    update(Constants.PREF_VALUE_ST, ST_MCU_1)
                }
                Constants.PREF_VALUE_OTA -> {
                    Log.e("update OTA", "OTA Update Start")
                    update(Constants.PREF_VALUE_OTA, "")
                }
                Constants.PREF_VALUE_APP -> {
                    Log.e("update APP", "APP Update Start")
                    update(Constants.PREF_VALUE_APP, "")
                }
                else -> {
                    //
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    suspend fun update(type: Int, port: String) {
        when (type) {
            Constants.PREF_VALUE_ST -> {
                Log.e("STUpdate", "Start Update $port")
                Commander.openSerialClient(port)

                val handler = Handler()
                val runnable = object : Runnable {
                    override fun run() {

                        if (port == ST_MCU_1 && !Commander.update_complete) {
                            handler.postDelayed(this, 5000)
                            return
                        }

                        activity?.runOnUiThread {
                            when (port) {
                                ST_MCU_0 ->
                                    textView_ttyS4_Progress.text =
                                        String.format("Sending UPDATE_READY to %s", port)
                                ST_MCU_1 ->
                                    textView_ttyS5_Progress.text =
                                        String.format("Sending UPDATE_READY to %s", port)
                            }
                        }
                        Commander.update_ready = false
                        for (i in 1..3) {
                            Commander.sendCommand(Command.UPDATE_READY, port)
                            Log.e("STUpdate $port", "Waiting for update_ready")
                            if (Commander.update_ready) {
                                Log.e("STUpdate $port", "update ready")
                                UpdateST(context!!, Commander.update_File.absolutePath, port)
                                return
                            }
                            Thread.sleep(3000)
                        }

                        //update_ready 에 대한 ACK 가 3회까지 오지 않았을 경우
                        for (i in 1..3) {
                            Log.e("STUpdate", "update_ready ACK Failed! reset MCU power!")
                            var out: BufferedWriter?
                            try {
                                var file = FileWriter("/sys/class/gpio_sw/PC1/data", false)
                                out = BufferedWriter(file)
                                out.write("0")
                                out.close()

                                Thread.sleep(1000)

                                file = FileWriter("/sys/class/gpio_sw/PC1/data", false)
                                out = BufferedWriter(file)
                                out.write("1")
                                out.close()


                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            Thread.sleep(3000)
                            if (Commander.update_ready) {
                                Log.e("STUpdate", "update ready - download mode")
                                UpdateST(context!!, Commander.update_File.absolutePath, port)
                                return
                            }
                        }
                        //3번 GPIO 리셋 후, Boot Complete 를 받지 못한 경우
                        activity?.runOnUiThread {
                            // 다이얼로그
                            val builder =
                                AlertDialog.Builder(
                                    ContextThemeWrapper(
                                        context,
                                        R.style.Theme_AppCompat_Light_Dialog
                                    )
                                )
                            builder.setTitle("ERROR with Serial $port")
                                .setMessage("Cannot get info from Serial port : $port")
                                .setCancelable(false)
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    activity?.finishAffinity()
                                }
                            builder.show()
                        }
                    }
                }
                // trigger first time
                handler.post(runnable)
            }

            Constants.PREF_VALUE_OTA -> {
                val handler = Handler()
                val runnable = Runnable {
                    try {
                        copyFile(File(Commander.update_File.absolutePath), File("/cache/ota.zip"))
                        Log.d(
                            "update",
                            "MainActivity copy file ${Commander.update_File.absolutePath} /cache/ota.zip"
                        )
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
                handler.postDelayed(runnable, 2000)
            }

            Constants.PREF_VALUE_APP -> {
                GlobalScope.launch {
                    try {
                        try {
                            //cache 로 update shell script 복사
                            copyFile(
                                File("storage/udisk3/${Constants.FILE_NAME_UPDATE_SHELL}"),
                                File("/cache/${Constants.FILE_NAME_UPDATE_SHELL}")
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("Exception", e.toString())
                            activity?.runOnUiThread {
                                update_text.text =
                                    "${Constants.FILE_NAME_UPDATE_SHELL} Not Found.\nClose App Update in 3sec."
                            }
                            SystemClock.sleep(3)
                            activity?.onBackPressed()
                        }
//                        val runtime = Runtime.getRuntime()
//                        val process = runtime.exec("/cache/${Constants.FILE_NAME_UPDATE_SHELL}\n")
//                        val input = BufferedReader(InputStreamReader(process.inputStream))
//                        val error = BufferedReader(InputStreamReader(process.errorStream))
//                        var log = ""
//                        var line = input.readLine()
//                        var line_e = error.readLine()
//                        while (line != null || line_e != null) {
//                            if(! line.isNullOrEmpty()){
//                                Log.e("line", line + "something")
//                                log += line + "\n"
//                                line = input.readLine()
//                            }
//                            if(! line_e.isNullOrEmpty()){
//                                Log.e("line_e", line_e + "something")
//                                log += line_e + "\n"
//                                line_e = error.readLine()
//                            }
//                            activity?.runOnUiThread {
//                                update_text.text = log
//                            }
//                        }

                        val pb = ProcessBuilder("/cache/${Constants.FILE_NAME_UPDATE_SHELL}")
                        pb.redirectErrorStream(true);
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        val process = pb.start()
                        Thread(ProcessTestRunnable(process)).start();
                        process.waitFor()
                        Log.e("APPUpdate", "process fin")
                        if (updated) {
                            val runtime = Runtime.getRuntime()
                            try {
                                val cmd = "reboot"
                                runtime.exec(cmd)
                            } catch (e: Exception) {
                                e.fillInStackTrace()
                            }
                        } else {
                            activity?.runOnUiThread {
                                // 다이얼로그
                                val builder =
                                    AlertDialog.Builder(
                                        ContextThemeWrapper(
                                            context,
                                            R.style.Theme_AppCompat_Light_Dialog
                                        )
                                    )
                                builder.setTitle("Update Result")
                                    .setMessage("Nothing Updated\n")
                                builder.show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("Exception Occurred", e.message)
                    }
                }
                return
            }
        }
    }

    var updateInfo = ""
    var updated = false
    val updateHandler = Handler {
        when (it.what) {
            MSG_UPDATE_MANAGER_SUCCEED -> {
                activity?.runOnUiThread {
                    Log.e("APPUpdate", "MANAGER updated!")
                    updateInfo += "MANAGER UPDATED\n"
                    updated = true;
                    update_text.text = updateInfo
                }
            }
            MSG_UPDATE_PURIUM_SUCCEED -> {
                activity?.runOnUiThread {
                    Log.e("APPUpdate", "PURIUM updated!")
                    updateInfo += "PURIUM UPDATED\n"
                    updated = true;
                    update_text.text = updateInfo
                }
            }
            MSG_UPDATE_MEDIA_SUCCEED -> {
                activity?.runOnUiThread {
                    Log.e("APPUpdate", "MEDIA updated!")
                    updateInfo += "MEIDA UPDATED\n"
                    updated = true;
                    update_text.text = updateInfo
                }
            }
            MSG_UPDATE_UPDATE_SUCCEED -> {
                activity?.runOnUiThread {
                    Log.e("APPUpdate", "UPDATE updated!")
                    updateInfo += "UPDATE UPDATED\n"
                    updated = true;
                    //update 앱이 업데이트 될 경우, 해당 텍스트를 표시할 수 없음.
                    //update_text.text = updateInfo
                }
            }
        }
        true
    }


    internal class ProcessTestRunnable(var p: Process) : Runnable {
        var br: BufferedReader? = null
        override fun run() {
            try {
                val isr = InputStreamReader(p.inputStream)
                br = BufferedReader(isr)
                var line: String? = null
                while (br!!.readLine().also { line = it } != null) {
                    Log.e("line", line)
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }

    }

    fun UpdateText(text: String, port: String) {
        activity?.runOnUiThread {
            when (port) {
                ST_MCU_0 ->
                    textView_ttyS4_Progress.text = text
                ST_MCU_1 ->
                    textView_ttyS5_Progress.text = text
            }
        }
    }

    fun setUpdateProgress(max: Int, port: String) {
        when (port) {
            ST_MCU_0 ->
                progressBar_ttyS4.max = max
            ST_MCU_1 ->
                progressBar_ttyS5.max = max
        }
    }

    fun UpdateProgress(progress: Int, port: String) {
        activity?.runOnUiThread {
            when (port) {
                ST_MCU_0 ->
                    progressBar_ttyS4.progress = progress
                ST_MCU_1 ->
                    progressBar_ttyS5.progress = progress
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