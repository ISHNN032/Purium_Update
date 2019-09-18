package com.neocartek.purium.update

import com.neocartek.purium.update.Constants.ACK_COUNT
import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.os.Message
import android.os.Handler
import android.os.SystemClock
import android.util.Log

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Arrays

import org.apache.http.util.ByteArrayBuffer

class UpdateST(context: Context, path: String) : UpdateMCU(context) {

    private val UPGRADE_READ = 0.toByte()
    private val UPGRADE_READ_SECCESS = 1.toByte()
    private val UPGRADE_INFO = 2.toByte()
    private val UPGRADE_DATA = 255.toByte()
    private val UPGRADE_NEXT_DATA = 3.toByte()
    private val UPGRADE_SUCCESS = 4.toByte()
    private val GET_STATE = 6.toByte()
    private val UPGRADE_VIEW = 254.toByte()
    private val DEFAULT_DATA = 0xFF.toByte()
    private val VIEW_NONE = 0
    private val VIEW_UPDATE = 1
    private val VIEW_FAIL = 2
    private val VIEW_SUCCESS = 3

    private var mAck_Count = ACK_COUNT

    private var _checkBuf: ByteArray? = null
    private var file_input: FileInputStream? = null
    private var g_index: Byte = 1
    private var g_count = 0

    private var mUpdateFile: File? = null

    private var m_path = ""
    private var u_sequence = 0



    private val READ_SIZE = 1024
    private val FILE_HEADER = 1024 * 2
    private val TIME_OUT = 1000
    private val PKG_SIZE = 1024

    private val PACKET_HEADER = 0xAA.toByte()
    private val PACKET_TAIL = 0x55.toByte()
    private val PACKET_TYPE_UPGRADE = 0x05.toByte()

    private val SIZE_CS_EXCEPT = 4
    private val SIZE_DATA_EXCEPT = 7

    private val PACKET_STOP = 0
    private val PACKET_TYPE = 1
    private val PACKET_LEN1 = 2
    private val PACKET_LEN2 = 3
    private val PACKET_CMD = 4
    private val PACKET_DATA = 5

    private val gData = ByteArrayBuffer(0)


    private val timeoutListenerGET_STATE = object : TimeOutHelper.ITimeOut {
        override fun onTimeOut(cmd: Byte, data: ByteArray?) {
            Log.e("ST UP", "--time out--")
            if (data != null) {
                sendMCUPacket(cmd, data)
            }
        }
    }

    private val timerHelper = TimeOutHelper()

    @SuppressLint("HandlerLeak")
    var mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val action = msg.what

            when (action.toByte()) {
                GET_STATE -> if (msg.arg1 == FD_RSPS.MODE_NORMAL.toInt()) {
                    Log.d("UPDATE","Start Update -> Let set download mode")
                    sendMCUPacket(FD_RQST.CMD_READ, byteArrayOf(FD_RQST.DATA_READ))
                }
                UPGRADE_INFO, UPGRADE_DATA -> Log.d("UPDATE","Update Info:" + msg.arg1 + ":" + msg.arg2)
                UPGRADE_VIEW -> if (msg.arg2 == VIEW_SUCCESS) {

                    resultUpdate(true)

                } else if (msg.arg2 == VIEW_FAIL) {
                    resultUpdate(false)
                }
            }
        }
    }

    private object FD_RQST {
        val CMD_READ = 0x00.toByte()
        val CMD_READ_SUCCESS = 0x01.toByte()
        val CMD_INFO = 0x02.toByte()
        val CMD_DATA = 0xff.toByte()
        val CMD_REBOOT = 0x05.toByte()

        val CMD_GET_STATE = 0x06.toByte()

        val DATA_READ = 0x00.toByte()
        val DATA_READ_SUCCESS = 0x00.toByte()
        val DATA_REBOOT = 0x00.toByte()
        val DATA_GET_STATE = 0x00.toByte()
    }

    private object FD_RSPS {
        val NON_ERROR = 0x00.toByte()
        val CHECKSUM_ERROR = 0x01.toByte()
        val MODE_ERROR = 0x02.toByte()
        val TIME_OUT = 0x02.toByte()
        val INDEX_ERROR = 0x03.toByte()
        val REQUEST_PKG = 0x00.toByte()
        val FLASH_WRITE_ERROR = 0x01.toByte()
        val SUCCESS = 0x00.toByte()

        val MODE_NORMAL = 0xFF.toByte()
        val MODE_DOWNLOAD = 0x00.toByte()
    }


    init {
        m_path = path
        if (openSerial("/dev/ttyS4", 19200)) {//115200
            mUpdateFile = File(m_path)

            Log.e("ST UP", "Start : " + mUpdateFile!!.absolutePath + " : " + mUpdateFile!!.isFile)

            checkStartUpdate()
        }
    }

    private fun checkStartUpdate() {
        mHandler.postDelayed({
            // TODO Auto-generated method stub
            if (mUpdateFile!!.isFile) {
                // 업데이트 시작 상태 저장
                //MyApplication.saveSharedPreferences(Constants.PREF_VALUE_ST);

                SystemClock.sleep(1500)

                sendMCUPacket(FD_RQST.CMD_GET_STATE, byteArrayOf(FD_RQST.DATA_GET_STATE))

            } else {
                // fail update (because don't find update file)
                resultUpdate(false)
            }
        }, 1500)
    }

    override fun ParsingPacket(buf: ByteArray, length: Int) {
        Log.d("UPDATE","ST MCU Data Parsing")
        dumpRawPacket("ParsingPacket::lengh:$length", buf, length)

        for (i in 0 until length) {
            when (buf[i]) {
                PACKET_HEADER -> gData.append(buf, i, 1)
                PACKET_TAIL -> if (gData.length() > 0) {
                    gData.append(buf, i, 1)
                    val _buf = gData.toByteArray()
                    val _data = ByteArray(_buf.size - SIZE_DATA_EXCEPT)
                    System.arraycopy(_buf, PACKET_DATA, _data, 0, _buf.size - SIZE_DATA_EXCEPT)

                    val len = (_buf[PACKET_LEN1].toInt() shl 4) + _buf[PACKET_LEN2].toInt() + SIZE_CS_EXCEPT
                    if (len <= gData.length()) {
                        if (_buf[PACKET_TYPE] == PACKET_TYPE_UPGRADE) {
                            // 정상 package
                            ParserData(_buf[PACKET_CMD], _data)
                        }
                        gData.clear()
                    }
                }
                else -> if (gData.length() > 0) {
                    gData.append(buf, i, 1)
                }
            }
        }
    }

    internal fun dumpRawPacket(strFunc: String, m_packet: ByteArray, length: Int) {
        var strTmp = "[$strFunc] data - "

        for (i in 0 until length)
            strTmp += String.format("%02x ", m_packet[i])

        Log.d("UpdateST", strTmp)
    }


    internal fun dumpRawPacket(strFunc: String, m_packet: ByteArray) {
        var strTmp = "[$strFunc] data - "

        for (i in m_packet.indices)
            strTmp += String.format("%02x ", m_packet[i])

        Log.d("UpdateST", strTmp)
    }

    fun ParserData(cmd: Byte, data: ByteArray) {
        // TODO Auto-generated method stub
        var rcv_ret = true

        when (cmd) {
            GET_STATE    //DOWN_READ
            -> if (data[0] == FD_RSPS.MODE_DOWNLOAD) {
                sendPostHandler(cmd.toInt(), data[0].toInt(), 0)
                mAck_Count = ACK_COUNT
                sendMCUPacket(FD_RQST.CMD_READ_SUCCESS, byteArrayOf(FD_RQST.DATA_READ_SUCCESS))
                Log.e("Upgrade", "STATE")
            } else if (data[0] == FD_RSPS.MODE_NORMAL) {
                mAck_Count = ACK_COUNT
                sendPostHandler(cmd.toInt(), data[0].toInt(), 0)
            } else {
                if (mAck_Count > 0) {
                    sendMCUPacket(FD_RQST.CMD_GET_STATE, byteArrayOf(FD_RQST.DATA_GET_STATE))
                    mAck_Count--
                } else {
                    sendPostHandler(UPGRADE_VIEW.toInt(), data[0].toInt(), VIEW_FAIL)
                    sendMCUPacket(FD_RQST.CMD_REBOOT, byteArrayOf(FD_RQST.DATA_REBOOT))
                }
            }

            UPGRADE_READ    //DOWN_READ
            -> if (data[0] == FD_RSPS.NON_ERROR) {
                mAck_Count = ACK_COUNT
                sendMCUPacket(FD_RQST.CMD_REBOOT, byteArrayOf(FD_RQST.DATA_REBOOT))
                Log.e("Upgrade", "READ")
            } else if (data[0] == FD_RSPS.MODE_ERROR) {
                mAck_Count = ACK_COUNT
                sendMCUPacket(FD_RQST.CMD_READ_SUCCESS, byteArrayOf(FD_RQST.DATA_READ_SUCCESS))
            } else {
                if (mAck_Count > 0) {
                    sendMCUPacket(FD_RQST.CMD_READ, byteArrayOf(FD_RQST.DATA_READ))
                    mAck_Count--
                } else {
                    sendMCUPacket(FD_RQST.CMD_REBOOT, byteArrayOf(FD_RQST.DATA_REBOOT))
                }
            }

            UPGRADE_READ_SECCESS -> if (data[0] == FD_RSPS.NON_ERROR) {
                mAck_Count = ACK_COUNT
                SendFileData(UPGRADE_INFO.toInt(), true)
                Log.e("Upgrade", "RE:SU")
            } else {
                if (mAck_Count > 0) {
                    sendMCUPacket(FD_RQST.CMD_READ_SUCCESS, byteArrayOf(FD_RQST.DATA_READ_SUCCESS))
                    mAck_Count--
                } else {
                    sendPostHandler(UPGRADE_VIEW.toInt(), data[0].toInt(), VIEW_FAIL)
                    sendMCUPacket(FD_RQST.CMD_REBOOT, byteArrayOf(FD_RQST.DATA_REBOOT))
                }
            }

            UPGRADE_INFO -> if (data[0] == FD_RSPS.NON_ERROR) {
                sendPostHandler(cmd.toInt(), g_index - 1, g_count)
                mAck_Count = ACK_COUNT
                SendFileData(UPGRADE_DATA.toInt(), true)
                Log.e("Upgrade", "INFO")
            } else {
                if (mAck_Count > 0) {
                    SendFileData(UPGRADE_INFO.toInt(), false)
                    mAck_Count--
                } else {
                    sendPostHandler(UPGRADE_VIEW.toInt(), data[0].toInt(), VIEW_FAIL)
                    sendMCUPacket(FD_RQST.CMD_REBOOT, byteArrayOf(FD_RQST.DATA_REBOOT))
                }
            }

            UPGRADE_DATA -> if (data[0] == FD_RSPS.NON_ERROR) {
                mAck_Count = ACK_COUNT
                Log.e("Upgrade", "DATA")
                sendPostHandler(cmd.toInt(), g_index - 1, g_count)
            } else {
                if (mAck_Count > 0) {
                    SendFileData(UPGRADE_DATA.toInt(), false)
                    mAck_Count--
                } else {
                    sendPostHandler(UPGRADE_VIEW.toInt(), data[0].toInt(), VIEW_FAIL)
                    sendMCUPacket(FD_RQST.CMD_REBOOT, byteArrayOf(FD_RQST.DATA_REBOOT))
                }
            }

            UPGRADE_NEXT_DATA -> if (data[0] == FD_RSPS.REQUEST_PKG) {
                mAck_Count = ACK_COUNT
                SendFileData(UPGRADE_DATA.toInt(), true)
                Log.e("Upgrade", "NEXT_DATA")
            } else {
                if (mAck_Count > 0) {
                    SendFileData(UPGRADE_DATA.toInt(), false)
                    mAck_Count--
                } else {
                    sendPostHandler(UPGRADE_VIEW.toInt(), data[0].toInt(), VIEW_FAIL)
                    sendMCUPacket(FD_RQST.CMD_REBOOT, byteArrayOf(FD_RQST.DATA_REBOOT))
                }
            }

            UPGRADE_SUCCESS -> {
                try {
                    file_input!!.close()
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }

                mAck_Count = ACK_COUNT
                //MyApplication.saveSharedPreferences(Constants.PREF_VALUE_NONE);
                sendPostHandler(UPGRADE_VIEW.toInt(), data[0].toInt(), VIEW_SUCCESS)
                Log.e("Upgrade", "UPGRADE:SU")
            }
            else -> {
                Log.w("UPDATE","Unknown Command !!!")
                rcv_ret = false
                dumpRawPacket("ParserData:unknown cmd=" + String.format("%02x ", cmd), data)
            }
        }
        if (rcv_ret)
            timerHelper.stopTimer()
    }

    fun sendPostHandler(action: Int, arg1: Int, arg2: Int) {
        val msg = Message()
        msg.what = action
        msg.arg1 = arg1
        msg.arg2 = arg2
        mHandler.sendMessage(msg)
    }

    // 업데이트 결과
    private fun resultUpdate(succes: Boolean) {

        //mUpdateFile = null;

        if (succes) { // Update Success !
            Log.e("Sucess", "message")
            // shared preferences 값 초기화

            sendMCUPacket(FD_RQST.CMD_REBOOT, byteArrayOf(FD_RQST.DATA_REBOOT))
            closeSerial()
            when(u_sequence){
                0->{
                    file_input!!.close()
                    file_input = null
                    g_index = 1
                    g_count = 0
                    mAck_Count = ACK_COUNT
                    mUpdateFile = null
                    _checkBuf = null
                    if (openSerial("/dev/ttyS5", 19200)) {//115200
                        mUpdateFile = File(m_path)
                        Log.e("ST UP", "Start : " + mUpdateFile!!.absolutePath + " : " + mUpdateFile!!.isFile)
                        u_sequence++
                        checkStartUpdate()
                    }
                }
                1->{
                    val runtime = Runtime.getRuntime()
                    try {
                        val cmd = "reboot"
                        runtime.exec(cmd)
                    } catch (e: Exception) {
                        e.fillInStackTrace()
                    }
                }
            }
        } else { // Update Fail

        }
    }


    private fun SendFileData(type: Int, _bRead: Boolean) {
        if (type == UPGRADE_INFO.toInt()) {
            val state = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {

                try {
                    if (_checkBuf == null || file_input == null) {
                        _checkBuf = ByteArray(READ_SIZE + 1)// FD_PKT.SIZE_CHECK_CRC_HEADER + _len];
                        file_input = FileInputStream(mUpdateFile!!)
                    }

                    if (_bRead) {
                        file_input!!.read(_checkBuf!!, 0, READ_SIZE)
                        file_input!!.skip((FILE_HEADER - READ_SIZE).toLong())
                    }

                    val _size = IntTo4Byte(mUpdateFile!!.length().toInt())
                    val _pkg = IntTo4Byte(PKG_SIZE)

                    g_count = (mUpdateFile!!.length().toInt() - FILE_HEADER) / PKG_SIZE
                    if ((mUpdateFile!!.length().toInt() - FILE_HEADER) % PKG_SIZE != 0) g_count++
                    val _cnt = IntTo4Byte(g_count)

                    val _timeout = IntTo4Byte(TIME_OUT)

                    sendMCUPacket(
                        FD_RQST.CMD_INFO,
                        byteArrayOf(
                            _size[0],
                            _size[1],
                            _size[2],
                            _size[3],
                            _pkg[0],
                            _pkg[1],
                            _pkg[2],
                            _pkg[3],
                            _cnt[0],
                            _cnt[1],
                            _cnt[2],
                            _cnt[3],
                            _timeout[0],
                            _timeout[1],
                            _timeout[2],
                            _timeout[3],
                            _checkBuf!![3],
                            _checkBuf!![4],
                            _checkBuf!![5],
                            _checkBuf!![6]
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        } else {
            try {
                //St Update가 하나이상 실행될 경우 _checkBuf가 null이 될 수 있음.
                if (_checkBuf == null || file_input == null) {
                    _checkBuf = ByteArray(READ_SIZE + 1)// FD_PKT.SIZE_CHECK_CRC_HEADER + _len];
                    file_input = FileInputStream(mUpdateFile!!)
                    //File Info 만큼(2K) Pointer 를 이동한다.
                    file_input!!.skip(FILE_HEADER.toLong()) // 2019.05.22 ISHNN
                }

                if (_bRead) {
                    Arrays.fill(_checkBuf!!, DEFAULT_DATA)
                    file_input!!.read(_checkBuf!!, 1, READ_SIZE)
                    _checkBuf!![0] = g_index++
                }

                sendMCUPacket(FD_RQST.CMD_DATA, _checkBuf!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun sendMCUPacket(cmd: Byte, data: ByteArray) {
        val buf = ByteArray(data.size + 7)
        buf[PACKET_STOP] = 0xAA.toByte()
        buf[PACKET_TYPE] = PACKET_TYPE_UPGRADE
        buf[PACKET_LEN1] = (data.size + 3 shr 8 and 0xFF).toByte()
        buf[PACKET_LEN2] = (data.size + 3 and 0xFF).toByte()
        buf[PACKET_CMD] = cmd
        System.arraycopy(data, 0, buf, PACKET_DATA, data.size)
        buf[data.size + 5] = mCheckSum(data, buf[PACKET_CMD], buf[PACKET_LEN1], buf[PACKET_LEN2]).inv().toByte()
        buf[data.size + 6] = 0x55.toByte()

        /*
        String _Log = "";
        for(int i=0; i<data.length + 7; i++) {
            _Log = _Log + "  " + buf[i];
        }
        */
        dumpRawPacket("sendMCUPacket", buf)

        write(buf)
        timerHelper.startTimer(timeoutListenerGET_STATE, 2000, cmd, data)
    }

    private fun mCheckSum(bytes: ByteArray, cmd: Byte, len1: Byte, len2: Byte): Int {
        var sum: Int = 0

        sum += (cmd.toInt() and 0xff)
        sum += (len1.toInt() and 0xff)
        sum += (len2.toInt() and 0xff)
        for (b in bytes) {
            sum += (b.toInt() and 0xff)
        }
        return sum
    }
}
