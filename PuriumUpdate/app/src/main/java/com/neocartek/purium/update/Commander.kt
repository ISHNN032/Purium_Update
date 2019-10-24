package com.neocartek.purium.update

import android.content.Context
import android.util.Log
import com.neocartek.purium.update.update_intro.SerialClient
import com.neocartek.purium.update.update_intro.*
import java.io.File
import java.util.*

// Uart Serial 통신 : SerialSender
// Server Socket 통신 : JsonClient
// 두 클래스를 통틀어 제어하는 Class

object Commander {
    private var mSerialClient: SerialClient? = null
    var update_path = ""
    var update_ready = false
    var update_complete = false

    var update_Type : Int = 0
    lateinit var update_File : File

    fun setSerialClient(context: Context) {
        mSerialClient = SerialClient(context)
    }
    fun openSerialClient(name: String) {
        when (name) {
            ST_MCU_0 -> {
                mSerialClient!!.openSerial(ST_MCU_0, SERIAL_FREQUENCY)
            }
            ST_MCU_1 -> {
                mSerialClient!!.openSerial(ST_MCU_1, SERIAL_FREQUENCY)
            }
        }
    }

    fun closeSerialClient(){
        mSerialClient?.closeSerial(ST_MCU_0)
        //mSerialClient?.closeSerial(ST_MCU_1)
    }
    fun closeSerialClient(name: String) {
        when (name) {
            ST_MCU_0 -> {
                mSerialClient?.closeSerial(ST_MCU_0)
            }
            ST_MCU_1 -> {
                mSerialClient?.closeSerial(ST_MCU_1)
            }
        }
    }

    fun sendCommand(command: Command, port : String, vararg data: Byte) {
        mSerialClient!!.sendPacket(command.b, port, data)
    }

    fun sendCommand(command: Command, port : String, data: ByteArray, subData: ByteArray) {
        mSerialClient!!.sendPacket(command.b, port, data, subData)
    }
}
