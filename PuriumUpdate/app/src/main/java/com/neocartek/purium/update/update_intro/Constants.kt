package com.neocartek.purium.update.update_intro

// 상수 정의

// 포트명
const val ST_MCU_0 = "/dev/ttyS4"
const val ST_MCU_1 = "/dev/ttyS5"

const val SERIAL_SERVICE = "serial"
const val SERIAL_FREQUENCY = 19200

const val PACKET_HEADER = 0xAA.toByte()
const val MAX_PACKET_SIZE = 8

const val DEFAULT_FAN_TIME = 4.toByte() // x5sec
const val LOWER_FAN_SWITCH_TIME = 30

enum class CommandState(val b: Byte) {
    RECEIVED(0x00),
    DONE(0x00),
    ERROR(0x00);
}

//명령어
enum class Command(val b: Byte) {
    //받는 명령
    ACKNOWLEDGE(0x00),
    SENSOR_STATUS(0x10),

    //보내는 명령
    FAN_MOVE_LEVEL(0x20),
    FAN_SET_LEVEL(0x21),
    FAN_POWER(0x30),
    FILTER_FAN_POWER(0x31),
    MONITOR_POWER(0x40),
    LED_POWER(0x50),
    VERSION_INFO(0x60),
    UPDATE_READY(0x61),
    UPDATE_REBOOT(0x62),
    UPDATE_BOOT_COMP(0x63);
}

//상부 팬은 상시 가동이므로 제어하지 않는다

// 양측 브러쉬 팬
enum class Fan(val b: Byte) {
    FAN_0(0b00000001.toByte()),
    FAN_1(0b00000010.toByte()),
    FAN_2(0b00000100.toByte()),
    FAN_3(0b00001000.toByte()),
    FAN_4(0b00010000.toByte()),
    FAN_5(0b00100000.toByte()),
    FAN_6(0b01000000.toByte()),
    FAN_7(0b10000000.toByte());
}

// 하단 집진팬
enum class LowerFan(val b: Byte) {
    FAN_0(0x01),
    FAN_1(0x02),
    FAN_2(0x03),
    FAN_3(0x04);
}

// 양 모니터 :
// 정보 모니터, 애니메이션 반복 재생 모니터
enum class Monitor(val b: Byte) {
    ANIMATION_MONITOR(0x01),
    SYSTEM_MONITOR(0x02);
}