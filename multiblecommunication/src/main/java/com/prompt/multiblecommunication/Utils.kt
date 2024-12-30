package com.prompt.multiblecommunication

object Utils {

    val fileName = "characteristics.txt"

    fun convertHexToAscci(hexValue: String): String {
        val output = StringBuilder()
        try {
            var i = 0
            while (i < hexValue.length) {
                val str = hexValue.substring(i, i + 2)
                output.append(str.trim { it <= ' ' }.toInt(16).toChar())
                i += 2
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output.toString()
    }

    fun byteToString(array: ByteArray): String {
        val sb = java.lang.StringBuilder()
        for (byteChar in array) {
            sb.append(String.format("%02x", byteChar))
        }
        return sb.toString()
    }

}