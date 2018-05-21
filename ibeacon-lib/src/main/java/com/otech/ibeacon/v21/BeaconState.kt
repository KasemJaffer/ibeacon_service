package com.otech.ibeacon.v21

import android.os.ParcelUuid
import android.os.SystemClock

class BeaconState(internal val address: String) {
    internal var companyId: Int? = null
    internal var uuid: ParcelUuid? = null
    internal var major = 0
    internal var minor = 0
    internal var calculatedRssiIn1m = 0
    private var mRssi = 0
    private var mLastTimestamp = 0L

    val isRecent: Boolean
        get() = mLastTimestamp + TIMEOUT > SystemClock.elapsedRealtime()

    val rssi: Float
        get() = mRssi.toFloat()

    fun setRssi(rssi: Int) {
        mLastTimestamp = SystemClock.elapsedRealtime()
        mRssi = rssi
    }

    fun getAccuracy(rssi: Float): Float {
        if (mLastTimestamp + INACTIVE < SystemClock.elapsedRealtime()) {
            return -1.0f
        } else if (rssi == 0.0f) {
            return -1.0f
        } else {
            val ratio = rssi / calculatedRssiIn1m.toFloat()
            return Math.max(Math.pow(ratio.toDouble(), 5.5).toFloat() - 5.0E-4f, 0.0f)
        }
    }

    fun matches(address: String): Boolean {
        return address == address
    }

    override fun equals(other: Any?): Boolean {
        return try {
            val o = other as BeaconState?
            address == o!!.address
        } catch (var3: ClassCastException) {
            false
        }

    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + (companyId ?: 0)
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + major
        result = 31 * result + minor
        result = 31 * result + calculatedRssiIn1m
        result = 31 * result + mRssi
        result = 31 * result + mLastTimestamp.hashCode()
        return result
    }

    companion object {
        private val TIMEOUT = 4000L
        private val INACTIVE = 4000L
    }
}