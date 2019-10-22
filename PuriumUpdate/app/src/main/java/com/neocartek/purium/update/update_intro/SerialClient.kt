package com.neocartek.purium.update.update_intro

import android.content.Context
import android.hardware.SerialManager
import android.hardware.SerialPort
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

// Serial 포트를 열어 주어진 명령어와 데이터를 송수신 하는 Class

class SerialClient(context: Context) {
    private var mSerialManager: SerialManager? = null
    private var mSerialPort_0: SerialPort? = null
    private lateinit var port0Context: CoroutineContext
    private var mSerialPort_1: SerialPort? = null
    private lateinit var port1Context: CoroutineContext

    private var mPacketManager = PacketManager()

    private var mInputBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE)
    private var mOutputBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE)

    init {
        //의미 없는 toString 이나, Android Service 에서 찾을 수 없어 SERIAL_SERVICE 그대로 사용할 경우 빨간줄 생김.
        mSerialManager = context.getSystemService(SERIAL_SERVICE.toString()) as SerialManager
    }

    // 포트를 열고 수신 스레드 실행
    fun openSerial(name: String, speed: Int) {
        Log.e("Intro", "Start Receive Coroutine : $name")
        when (name) {
            ST_MCU_0 -> {
                mSerialPort_0 = mSerialManager!!.openSerialPort(name, speed)
                port0Context = GlobalScope.launch {
                    receiveCoroutine()
                }
            }
            ST_MCU_1 -> {
                mSerialPort_1 = mSerialManager!!.openSerialPort(name, speed)
                port1Context = GlobalScope.launch {
                    receiveCoroutine()
                }
            }
        }
    }

    fun closeSerial(name: String) {
        when (name) {
            ST_MCU_0 -> {
                mSerialPort_0?.close()
                mSerialPort_0 = null
                port0Context.cancel()
            }
            ST_MCU_1 -> {
                mSerialPort_1?.close()
                mSerialPort_1 = null
                port1Context.cancel()
            }
        }
    }

    // 명령어를 Packet 형태로 Wrapping 하여 전송
    fun sendPacket(command: Byte, data: ByteArray) {
        var packet = mPacketManager.wrapPacket(command, data)
        try {
            mOutputBuffer.clear()
            mOutputBuffer.put(packet)
            mSerialPort_0!!.write(mOutputBuffer, packet.size)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendPacket(command: Byte, data: ByteArray, subData: ByteArray) {
        var packet = mPacketManager.wrapPacket(command, data)
        var subPacket = mPacketManager.wrapPacket(command, subData)
        try {
            mOutputBuffer.clear()
            mOutputBuffer.put(packet)
            mSerialPort_0!!.write(mOutputBuffer, packet.size)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // RecvThread 로 부터 들어오는 데이터를 처리
    fun readPacket(data: ByteArray, length: Int) {
        var packetlenth = 0
        var packet: List<Byte> = listOf()
        var ispacket = false

        for (i in data.indices) {
            var b = data[i] //임시 바이트 단위

            if (packetlenth == 0) {
                //기존의 packet 이 있는 경우: 해석 후, 처리한다.
                if (packet.isNotEmpty() && ispacket) {
                    mPacketManager.unwrapPacket(packet.toByteArray())
                    ispacket = false
                }
                //헤더를 발견한 경우: 헤더를 기준으로 list 를 생성하고 남은 길이를 받아온다.
                if (b == 0xAA.toByte()) {
                    packet = listOf(b)
                    packetlenth = data[i + 1].toInt() - 1
                    ispacket = true
                }
            } else {
                //패킷 길이 안의 데이터: packet 에 추가하며 남은 길이를 확인한다.
                packet += b
                --packetlenth
            }
        }
    }


    // 시리얼 포트가 닫힐 때 까지 데이터를 받는 스레드
    private fun receiveCoroutine() {
        var ret = 0
        var subRet = 0
        var buffer = ByteArray(MAX_PACKET_SIZE)
        while (mSerialPort_0 != null) {
            try {
                //mInputBuffer 에 값을 저장하고, ret에 길이를 반환하는 구조.
                mInputBuffer.clear()
                ret = mSerialPort_0!!.read(mInputBuffer)

                //subRet 의 값을 읽어들인 InputBuffer 를 최종으로 사용한다.
                mInputBuffer.get(buffer, 0, ret)
            } catch (e: IOException) {
            } catch (ue: BufferUnderflowException) { }
            if (ret > 0) {
                readPacket(buffer, ret)
            } else {
            }
        }
    }
}