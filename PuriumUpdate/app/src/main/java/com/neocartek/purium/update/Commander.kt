package com.neocartek.purium.update

import android.content.Context
import android.util.Log
import com.neocartek.purium.update.update_intro.SerialClient
import com.neocartek.purium.update.update_intro.*
import java.util.*

// Uart Serial 통신 : SerialSender
// Server Socket 통신 : JsonClient
// 두 클래스를 통틀어 제어하는 Class

object Commander {
    private var mSerialClient: SerialClient? = null
    var update_path = ""
    var update_ready = false

    fun setSerialClient(context: Context) {
        mSerialClient = SerialClient(context)
        mSerialClient!!.openSerial(ST_MCU_0, SERIAL_FREQUENCY)
        mSerialClient!!.openSerial(ST_MCU_1, SERIAL_FREQUENCY)
    }

    fun closeSerialClient(){
        mSerialClient?.closeSerial(ST_MCU_0)
        mSerialClient?.closeSerial(ST_MCU_1)
    }

    fun sendCommand(command: Command, vararg data: Byte) {
        mSerialClient!!.sendPacket(command.b, data)
    }

    fun sendCommand(command: Command, data: ByteArray, subData: ByteArray) {
        mSerialClient!!.sendPacket(command.b, data, subData)
    }
}
