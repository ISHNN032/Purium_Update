package com.neocartek.purium.update.update
import android.os.Handler

class TimeOutHelper {

    private var m_cmd: Byte = 0
    private var m_data: ByteArray? = null

    private var listener: ITimeOut? = null

    private val timeoutHanldler = Handler()

    private val timer = Runnable {
        stopTimer()
        if (listener != null) {
            listener!!.onTimeOut(m_cmd, m_data)
        }
    }

    fun startTimer(timeoutListener: ITimeOut, delay: Long, cmd: Byte, data: ByteArray) {
        m_cmd = cmd
        m_data = data
        listener = timeoutListener
        timeoutHanldler.postDelayed(timer, delay)
    }

    fun stopTimer() {
        timeoutHanldler.removeCallbacksAndMessages(null)
    }

    fun unRegisterListener() {
        listener = null
    }

    interface ITimeOut {
        fun onTimeOut(cmd: Byte, data: ByteArray?)
    }

}

