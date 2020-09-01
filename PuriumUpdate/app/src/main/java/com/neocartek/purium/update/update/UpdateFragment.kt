package com.neocartek.purium.update.update

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.RecoverySystem
import android.os.SystemClock
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.view.ContextThemeWrapper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.neocartek.purium.update.BuildConfig
import com.neocartek.purium.update.Commander
import com.neocartek.purium.update.Constants
import com.neocartek.purium.update.R
import com.neocartek.purium.update.update_intro.Command
import com.neocartek.purium.update.update_intro.ST_MCU_0
import com.neocartek.purium.update.update_intro.ST_MCU_1
import kotlinx.android.synthetic.main.fragment_update.*
import kotlinx.android.synthetic.main.fragment_update.progressBar_ttyS4
import kotlinx.android.synthetic.main.fragment_update_app.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.*
import org.apache.commons.io.IOUtils;


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
                val handler = Handler()
                val runnable = Runnable {
                    try {
                        try {
                            //cache 로 update shell script 복사
                            copyFile(File("${Commander.update_path}/${Constants.FILE_NAME_UPDATE_SHELL}"),
                                File("/cache/${Constants.FILE_NAME_UPDATE_SHELL}"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("Exception", e.toString())
                        }

                        val runtime = Runtime.getRuntime()
                        val process = runtime.exec("/cache/${Constants.FILE_NAME_UPDATE_SHELL}\n")
                        val input = BufferedReader(InputStreamReader(process.inputStream))
                        val error = BufferedReader(InputStreamReader(process.errorStream))
                        var log = ""
                        var line = input.readLine()
                        var line_e = error.readLine()
                        while (line != null || line_e != null) {
                            if(! line_e.isNullOrEmpty()){
                                Log.e("line", line)
                                log += line + "\n"
                                line = input.readLine()
                            }
                            if(! line_e.isNullOrEmpty()){
                                Log.e("line_e", line_e)
                                log += line_e + "\n"
                                line_e = error.readLine()
                            }
                            activity?.runOnUiThread {
                                update_text.text = log
                            }
                        }
                        input.close()
                        error.close()
                        process.waitFor()
                        Log.e("process", "fin")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                handler.postDelayed(runnable, 2000)
            }
        }
    }

    private fun uninstallPackage(context: Context, packageName: String): Boolean {
        val packageManger: PackageManager = context.packageManager
        val packageInstaller =
            packageManger.packageInstaller
        val params =
            PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
        Log.e("uninstallPackage", "setAppPackageName")
        params.setAppPackageName(packageName)
        var sessionId = 0
        sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        Log.e("uninstallPackage", "packageInstaller.uninstall")
        packageInstaller.uninstall(
            packageName, PendingIntent.getBroadcast(
                context, sessionId,
                Intent("Android.intent.action.MAIN"), 0
            ).intentSender
        )
        Log.e("uninstallPackage", "uninstall Fin")
        return true
    }

    private fun installPackage(context: Context,
        packageName: String, packagePath: String): Boolean {
//        val apkUri = FileProvider.getUriForFile(
//            context!!,
//            "${BuildConfig.APPLICATION_ID}.provider",
//            File(apkPath)
//        )
//        val intent = Intent(Intent.ACTION_VIEW)   // 2
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)  // 3
//        intent.setDataAndType(
//            apkUri,
//            "application/vnd.android.package-archive"
//        )  // 4
//        startActivity(intent)

        val packageManger = context.packageManager
        val packageInstaller =
            packageManger.packageInstaller
        val params =
            PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
        Log.e("installPackage", "setAppPackageName")
        params.setAppPackageName(packageName)
        return try {
            val sessionId = packageInstaller.createSession(params)
            val session =
                packageInstaller.openSession(sessionId)
            val out = session.openWrite("$packageName.apk", 0, -1)
            Log.e("installPackage", "IOUtils.copy")
            IOUtils.copy(
                File(packagePath).inputStream(),
                out
            ) //read the apk content and write it to out
            session.fsync(out)
            out.close()
            Log.e("installPackage", "installing...")
            session.commit(
                PendingIntent.getBroadcast(
                    context, sessionId,
                    Intent("Android.intent.action.MAIN"), 0
                ).intentSender
            )
            Log.e("installPackage", "install Fin")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("installPackage", "install Fail")
            false
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