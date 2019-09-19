package com.neocartek.purium.update.update

import android.util.Log
import android.content.Context
import android.hardware.SerialManager
import android.hardware.SerialPort
import com.neocartek.purium.update.Constants
import java.nio.ByteBuffer

import java.io.IOException

open class UpdateMCU internal constructor(var mContext: Context) {

    private val mSerialManager: SerialManager = mContext.getSystemService(Constants.SERIAL_SERVICE) as SerialManager
    private var mSerialPort: SerialPort? = null

    private var mInputBuffer: ByteBuffer? = null
    private var mOutputBuffer: ByteBuffer? = null

    protected val isOpen: Boolean
        get() = mSerialPort != null

    private var mRThread : RecvThread? = null;



    protected fun openSerial(name: String, speed: Int): Boolean {
        var ret = false
        /**
         * String[] ports = mSerialManager.getSerialPorts();
         * DEBUG.d("getSerialPorts ="+ports.length);
         * for(int i=0;i<ports.length></ports.length>;i++){
         * DEBUG.d("UpdateMCU port["+i+"]="+ports[i]);
         * }
         */
        try {
            mSerialPort = mSerialManager.openSerialPort(name, speed)

            mInputBuffer = ByteBuffer.allocateDirect(1032)
            mOutputBuffer = ByteBuffer.allocateDirect(1032)

            mRThread = RecvThread()
            mRThread?.start()

            ret = true
        } catch (e: IOException) {
            mSerialPort = null
            e.printStackTrace()
        }

        return ret
    }

    protected fun closeSerial() {
        mRThread?.interrupt()
        if (mSerialPort != null) {
            try {
                mSerialPort!!.close()
            } catch (e: IOException) {
            }

            mSerialPort = null
        }
    }

    protected fun write(buf: ByteArray) {
        if (mSerialPort != null) {
            try {
                mOutputBuffer!!.clear()
                mOutputBuffer!!.put(buf)
                mSerialPort!!.write(mOutputBuffer, buf.size)
            } catch (e: IOException) {

            }

        }
    }

    private inner class RecvThread : Thread() {
        internal var ret = 0
        internal var buffer = ByteArray(1024)
        override fun run() {
            while (mSerialPort != null) {
                try {
                    mInputBuffer!!.clear()
                    ret = mSerialPort!!.read(mInputBuffer)
                    mInputBuffer!!.get(buffer, 0, ret)
                    Log.v("UPDATE","RecvThread ret=$ret")
                } catch (e: IOException) {
                    Log.e("UPDATE","RecvThread IOException")
                }

                if (ret > 0) {
                    //DEBUG.v("RecvThread ret="+ret);
                    ParsingPacket(buffer, ret)
                } else {
                    Log.e("UPDATE","RecvThread ret=$ret")
                }
            }
        }
    }

    open fun ParsingPacket(buf: ByteArray, len: Int) {}

    companion object {


        fun IntTo4Byte(i: Int): ByteArray {
            val ito4Byte = ByteArray(4)
            ito4Byte[0] = (i and 0xFF).toByte()
            ito4Byte[1] = (i shr 8 and 0xFF).toByte()
            ito4Byte[2] = (i shr 16 and 0xFF).toByte()
            ito4Byte[3] = (i shr 24 and 0xFF).toByte()
            return ito4Byte
        }
    }
}
