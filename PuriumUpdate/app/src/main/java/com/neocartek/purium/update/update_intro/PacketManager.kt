package com.neocartek.purium.update.update_intro

import android.util.Log
import com.neocartek.purium.update.Commander
import com.neocartek.purium.update.Constants
import com.neocartek.purium.update.MainActivity
import com.neocartek.purium.update.update.UpdateST
import java.io.File


// Serial 통신을 위해 규격에 맞도록 Packet 을 생성하는 것을 도와주는 Class

class PacketManager {
    // header 와 length, checksum 을 붙여 Packet 을 생성
    fun wrapPacket(command: Byte, data: ByteArray) : ByteArray{
        var length = 3 + data.size + 1  //헤더/길이/커맨드/ " 데이터 " /Checksum
        if(length > MAX_PACKET_SIZE)
            throw Exception("Packet Data is Too Big")

        var buffer = ByteArray(length)

        buffer[0] = PACKET_HEADER
        buffer[1] = length.toByte()
        buffer[2] = command
        for(i in data.indices){
            buffer[3+i] = data[i]
        }

        buffer[length-1] = checkSum(buffer.drop(1))
        return buffer
    }

    fun unwrapPacket(buffer : ByteArray){
        var byteString : String = ""
        for(b in buffer){
            var hex = Integer.toHexString(b.toUByte().toInt())
            byteString += "| $hex "
        }
        Log.e("Received Byte", byteString)

        var commandByte = buffer[2]
        var data = buffer.copyOfRange(3,buffer.size-1)
        byteString = ""
        for(b in data){
            var hex = Integer.toHexString(b.toUByte().toInt())
            byteString += "| $hex "
        }
        Log.e("Received Data", byteString)

        var checksumByte = buffer[buffer.size-1]
        for (command in Command.values()){
            if(commandByte == command.b){
                var checksum = checkSum(buffer.copyOfRange(1,buffer.size-1).toList())
                if(checksum == checksumByte)
                    Log.e("checksum","c: $checksum | b: $checksumByte | correct")
                else
                    Log.e("checksum","c: $checksum | b: $checksumByte | incorrect")

                when(command){
                    Command.ACKNOWLEDGE -> {
                        when(data[0]){
                            Command.UPDATE_READY.b -> {
                                Commander.sendCommand(Command.UPDATE_REBOOT)
                                Log.e("UPDATE","READY ST MCU . . .")
                            }
                            Command.UPDATE_REBOOT.b -> {
                                Commander.closeSerialClient()
                                Log.e("UPDATE","REBOOT ST MCU . . .")
                                Commander.update_ready = true
                            }
                        }
                    }
                    Command.UPDATE_BOOT_COMP -> {
                        Log.e("UPDATE","ST MCU BOOT COMPLETED ! !")
                        //Todo Update ST MCU
                    }
                    Command.VERSION_INFO -> {
                        var versionInfo = ""
                        for(b in data){
                            versionInfo = b.toInt().toString()
                        }
                        Log.e("ST_VERSION", data[0].toInt().toString())
                    }
                }
            }
        }
    }

    //Byte List 를 모두 더해 0xFF로 xor 하여 결과를 전송
    private fun checkSum(packet : List<Byte>) : Byte {
        var checksum = 0
        for(v in packet){
            checksum += v.toUByte().toInt()
        }
        checksum.toByte()
        return checksum.xor(0xFF).toByte()
    }
}