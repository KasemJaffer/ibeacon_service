package com.otech.ibeacon.v21.utils


object ParserUtils {

    fun decodeHalfUuid(data: ByteArray, start: Int): Long {
        return unsignedByteToLong(data[start]).shl(56) +
                unsignedByteToLong(data[start + 1]).shl(48) +
                unsignedByteToLong(data[start + 2]).shl(40) +
                unsignedByteToLong(data[start + 3]).shl(32) +
                unsignedByteToLong(data[start + 4]).shl(24) +
                unsignedByteToLong(data[start + 5]).shl(16) +
                unsignedByteToLong(data[start + 6]).shl(8) +
                unsignedByteToLong(data[start + 7])
    }

    fun decodeUint16BigEndian(data: ByteArray, start: Int): Int {
        val b1 = data[start].toInt() and 255
        val b2 = data[start + 1].toInt() and 255
        return b1 or (b2.shl(8))
    }

    fun decodeUint16LittleEndian(data: ByteArray, start: Int): Int {
        val b1 = data[start].toInt() and 255
        val b2 = data[start + 1].toInt() and 255
        return b1.shl(8) or b2
    }

    fun unsignedByteToInt(b: Byte): Int {
        return b.toInt() and 255
    }

    fun unsignedByteToLong(b: Byte): Long {
        return (b.toInt() and 255).toLong()
    }
}
