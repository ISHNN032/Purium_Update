package com.neocartek.purium.update

object Constants {
    val FILE_NAME_CPU_OTA = "update_ota_file.zip"
    val FILE_NAME_MCU_ST = "update_st_file.bin"

    val FILE_NAME_UPDATE_SHELL = "usb_update.sh"
    val FILE_NAME_APP_PURIUM = "purium.apk"
    val FILE_NAME_APP_MANAGER = "purium_manager.apk"
    val FILE_NAME_APP_MEDIA = "purium_media_view.apk"
    val FILE_NAME_APP_UPDATE = "purium_update.apk"
    val NUM_OF_APPS = 4

    const val PACKAGE_NAME_MANAGER = "com.neocartek.purium.manager"
    const val PACKAGE_NAME_UPDATE = "com.neocartek.purium.update"
    const val PACKAGE_NAME_PURIUM = "com.example.test"
    const val PACKAGE_NAME_MEDIA = "com.awesomeit.purium"

    const val MSG_UPDATE_MANAGER_SUCCEED = 0
    const val MSG_UPDATE_PURIUM_SUCCEED = 1
    const val MSG_UPDATE_MEDIA_SUCCEED = 2
    const val MSG_UPDATE_UPDATE_SUCCEED = 3

    val FILE_PATH_STORAGE = "storage"

    val PREF_KEY_UPDATE = "pref_key_update"
    val PREF_VALUE_NONE = 0
    val PREF_VALUE_ST = 1
    val PREF_VALUE_APP = 2
    val PREF_VALUE_OTA = 3

    val SERIAL_SERVICE = "serial"

    val ACK_COUNT = 3
}
