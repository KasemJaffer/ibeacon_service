package com.otech.ibeacon.v21


import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationManager
import android.os.*
import android.util.Log
import android.util.SparseArray
import com.otech.ibeacon.BeaconRegion
import com.otech.ibeacon.ServiceProxy
import com.otech.ibeacon.v21.utils.ParserUtils
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

@TargetApi(21)
class BeaconRegionState {
    private var mTimestamp = 0L
    private var mState = 0
    private val mAnyUuid: Boolean
    private val mMsb: Long
    private val mLsb: Long
    private val mMajor: Int
    private val mMinor: Int
    private val mCompanyId: Int
    private val mMessenger: Messenger?
    private val mOwnerPackageName: String?
    private val mNotificationId: Int
    private val mNotification: Notification?
    private val mBeacons = SparseArray<BeaconState>()
    internal val filter: ScanFilter

    constructor(uuid: UUID, major: Int, minor: Int, companyId: Int, ownerPackageName: String, notification: Notification) {
        val bb = ByteBuffer.wrap(ByteArray(23))
        bb.put(2.toByte())
        bb.put(21.toByte())
        if (uuid !== BeaconRegion.ANY_UUID) {
            mAnyUuid = false
            mMsb = uuid.mostSignificantBits
            mLsb = uuid.leastSignificantBits
            bb.putLong(mMsb)
            bb.putLong(mLsb)
            filter = if (major != BeaconRegion.ANY) {
                bb.putShort(major.toShort())
                if (minor != BeaconRegion.ANY) {
                    bb.putShort(minor.toShort())
                    ScanFilter.Builder().setManufacturerData(companyId, bb.array(), UUID_MAJOR_MINOR_BEACON_MASK).build()
                } else {
                    ScanFilter.Builder().setManufacturerData(companyId, bb.array(), UUID_MAJOR_BEACON_MASK).build()
                }
            } else {
                ScanFilter.Builder().setManufacturerData(companyId, bb.array(), UUID_BEACON_MASK).build()
            }
        } else {
            mAnyUuid = true
            mLsb = 0L
            mMsb = mLsb
            filter = ScanFilter.Builder().setManufacturerData(companyId, bb.array(), ANY_BEACON_MASK).build()
        }

        mMajor = major
        mMinor = minor
        mCompanyId = companyId
        mOwnerPackageName = ownerPackageName
        mNotification = notification
        mNotificationId = ownerPackageName.hashCode() xor uuid.hashCode() xor (major shl 16) xor minor
        mMessenger = null
        mState = STATE_NEW
        mTimestamp = SystemClock.elapsedRealtime()
    }

    constructor(uuid: UUID?, major: Int, minor: Int, companyId: Int, messenger: Messenger) {
        val bb = ByteBuffer.wrap(ByteArray(23))
        bb.put(2.toByte())
        bb.put(21.toByte())
        if (uuid !== BeaconRegion.ANY_UUID) {
            mAnyUuid = false
            mMsb = uuid!!.mostSignificantBits
            mLsb = uuid.leastSignificantBits
            bb.putLong(mMsb)
            bb.putLong(mLsb)
            filter = if (major != BeaconRegion.ANY) {
                bb.putShort(major.toShort())
                if (minor != BeaconRegion.ANY) {
                    bb.putShort(minor.toShort())
                    ScanFilter.Builder().setManufacturerData(companyId, bb.array(), UUID_MAJOR_MINOR_BEACON_MASK).build()
                } else {
                    ScanFilter.Builder().setManufacturerData(companyId, bb.array(), UUID_MAJOR_BEACON_MASK).build()
                }
            } else {
                ScanFilter.Builder().setManufacturerData(companyId, bb.array(), UUID_BEACON_MASK).build()
            }
        } else {
            mAnyUuid = true
            mLsb = 0L
            mMsb = mLsb
            filter = ScanFilter.Builder().setManufacturerData(companyId, bb.array(), ANY_BEACON_MASK).build()
        }

        mMajor = major
        mMinor = minor
        mCompanyId = companyId
        mMessenger = messenger
        mOwnerPackageName = null
        mNotification = null
        mNotificationId = 0
        mState = STATE_NEW
        mTimestamp = SystemClock.elapsedRealtime()
    }

    fun hasJustEntered(): Boolean {
        val now = SystemClock.elapsedRealtime()
        val justEntered = mState == STATE_OUTSIDE || mState == STATE_NEW && mTimestamp < now - INIT_DELAY
        mState = STATE_INSIDE
        mTimestamp = now
        return justEntered
    }

    fun hasJustExited(now: Long): Boolean {
        val justExited = mState == STATE_INSIDE && mTimestamp < now - TIMEOUT
        if (justExited || mState == STATE_NEW && mTimestamp < now - INIT_DELAY) {
            mState = STATE_OUTSIDE
        }

        return justExited
    }

    fun beaconFound(result: ScanResult) {
        synchronized(mBeacons) {
            val address = result.device.address
            val beacon = mBeacons.get(address.hashCode(), BeaconState(address))
            mBeacons.put(address.hashCode(), beacon)
            val manufacturerData = result.scanRecord!!.getManufacturerSpecificData(mCompanyId)
            val msb = ParserUtils.decodeHalfUuid(manufacturerData!!, 2)
            val lsb = ParserUtils.decodeHalfUuid(manufacturerData, 10)
            if (beacon.uuid == null
                    || beacon.uuid?.uuid?.mostSignificantBits != msb
                    || beacon.uuid?.uuid?.leastSignificantBits != lsb) {
                beacon.uuid = ParcelUuid(UUID(msb, lsb))
            }

            beacon.major = ParserUtils.decodeUint16LittleEndian(manufacturerData, 18)
            beacon.minor = ParserUtils.decodeUint16LittleEndian(manufacturerData, 20)
            beacon.calculatedRssiIn1m = manufacturerData[22].toInt()
            beacon.companyId = mCompanyId
            beacon.setRssi(result.rssi)
        }
    }

    fun sendBeaconsInRegionNotification(): Boolean {
        try {
            val message = Message.obtain(null, ServiceProxy.MSG_BEACONS_IN_REGION)
            message.obj = if (!mAnyUuid) ParcelUuid(UUID(mMsb, mLsb)) else null
            message.arg1 = mMajor
            message.arg2 = mMinor
            message.data.putInt(ServiceProxy.EXTRA_COMPANY_ID, mCompanyId)
            synchronized(mBeacons) {
                val beacons = mBeacons
                val beaconsCount = beacons.size()
                val args = message.data
                val addresses = arrayOfNulls<String>(beaconsCount)
                val uuids = arrayOfNulls<ParcelUuid>(beaconsCount)
                val numbers = IntArray(beaconsCount)
                val accuracies = FloatArray(beaconsCount)
                val rssis = IntArray(beaconsCount)

                var i = 0
                var beacon: BeaconState
                while (i < beaconsCount) {
                    beacon = beacons.valueAt(i)
                    addresses[i] = beacon.address
                    uuids[i] = beacon.uuid
                    numbers[i] = beacon.major shl 16 or beacon.minor
                    val rssi = beacon.rssi
                    rssis[i] = rssi.toInt()
                    accuracies[i] = beacon.getAccuracy(rssi)
                    ++i
                }

                args.putInt(ServiceProxy.EXTRA_COUNT, beaconsCount)
                args.putStringArray(ServiceProxy.EXTRA_ADDRESSES, addresses)
                args.putParcelableArray(ServiceProxy.EXTRA_UUIDS, uuids)
                args.putIntArray(ServiceProxy.EXTRA_NUMBERS, numbers)
                args.putFloatArray(ServiceProxy.EXTRA_ACCURACIES, accuracies)
                args.putIntArray(ServiceProxy.EXTRA_RSSI_VALUES, rssis)
                mMessenger!!.send(message)

                i = 0
                while (i < beacons.size()) {
                    beacon = beacons.valueAt(i)
                    if (!beacon.isRecent) {
                        beacons.removeAt(i--)
                    }
                    ++i
                }

                return true
            }
        } catch (var16: RemoteException) {
            Log.w(TAG, "Messenger is dead. Marking beacon as dead.")
            return false
        }

    }

    fun sendRegionEnteredNotification(nm: NotificationManager): Boolean {
        if (mNotification != null) {
            nm.notify(mNotificationId, mNotification)
        } else {
            try {
                val message = Message.obtain(null, ServiceProxy.MSG_REGION_ENTERED)
                message.obj = if (!mAnyUuid) ParcelUuid(UUID(mMsb, mLsb)) else null
                message.arg1 = mMajor
                message.arg2 = mMinor
                message.data.putInt(ServiceProxy.EXTRA_COMPANY_ID, mCompanyId)
                mMessenger!!.send(message)
            } catch (var3: RemoteException) {
                Log.w(TAG, "Messenger is dead. Marking beacon as dead.")
                return false
            }

        }

        return true
    }

    fun sendRegionExitedNotification(nm: NotificationManager): Boolean {
        if (mNotification != null) {
            nm.cancel(mNotificationId)
        } else {
            try {
                val message = Message.obtain(null, ServiceProxy.MSG_REGION_EXITED)
                message.obj = if (!mAnyUuid) ParcelUuid(UUID(mMsb, mLsb)) else null
                message.arg1 = mMajor
                message.arg2 = mMinor
                message.data.putInt(ServiceProxy.EXTRA_COMPANY_ID, mCompanyId)
                mMessenger!!.send(message)
            } catch (var3: RemoteException) {
                Log.w(TAG, "Messenger is dead. Marking beacon as dead.")
                return false
            }

        }

        return true
    }

    fun matches(messenger: Messenger): Boolean {
        return messenger == mMessenger
    }

    fun matches(result: ScanResult): Boolean {
        val parsedData = result.scanRecord!!.getManufacturerSpecificData(mCompanyId)
        if (parsedData == null) {
            return false
        } else {
            val data = filter.manufacturerData
            val dataMask = filter.manufacturerDataMask

            for (i in data!!.indices) {
                if (dataMask!![i] and parsedData[i] != dataMask[i] and data[i]) {
                    return false
                }
            }

            return true
        }
    }

    fun matchesStrict(msb: Long, lsb: Long, major: Long, minor: Long, companyId: Int): Boolean {
        return mCompanyId == companyId && mMsb == msb && mLsb == lsb && mMajor.toLong() == major && mMinor.toLong() == minor
    }

    fun matchesStrict(msb: Long, lsb: Long, major: Long, minor: Long, companyId: Int, ownerPackageName: String): Boolean {
        return matchesStrict(msb, lsb, major, minor, companyId) && mOwnerPackageName != null && mOwnerPackageName == ownerPackageName
    }

    fun hasMessenger(): Boolean {
        return mMessenger != null
    }

    override fun toString(): String {
        return "[company: ${Integer.toHexString(mCompanyId)} filter: $filter]"
    }

    override fun equals(other: Any?): Boolean {
        return try {
            val o = other as BeaconRegionState?
            filter == o!!.filter
                    && (mMessenger == null
                    && o.mMessenger == null || mMessenger != null
                    && mMessenger == o.mMessenger)
        } catch (var3: ClassCastException) {
            false
        }
    }

    override fun hashCode(): Int {
        var result = filter.hashCode()
        result = 31 * result + (mMessenger?.hashCode() ?: 0)
        return result
    }

    companion object {
        private val TAG = "BeaconRegionState"
        val UUID_BEACON_MASK = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0)
        val UUID_MAJOR_BEACON_MASK = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0)
        val UUID_MAJOR_MINOR_BEACON_MASK = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0)
        val ANY_BEACON_MASK = byteArrayOf(-1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        private val TIMEOUT = 20000L
        private val INIT_DELAY = 2000L
        private val STATE_OUTSIDE = 0
        private val STATE_INSIDE = 1
        private val STATE_NEW = 2
    }
}