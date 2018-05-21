package com.otech.ibeacon

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.util.SparseArray
import java.util.*
import kotlin.collections.ArrayList

abstract class BeaconServiceConnection : ServiceConnection {
    private val mRegionsByHash = SparseArray<BeaconRegion>()
    private val mBeaconsByAddressHash = SparseArray<Beacon>()
    private val mBeaconsMessengers = HashMap<BeaconsListener, Messenger>()
    private val mRegionMessengers = HashMap<BeaconServiceConnection.RegionListener, Messenger>()
    private var mService: Messenger? = null

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        mService = Messenger(service)
        onServiceConnected()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        mService = null
        onServiceDisconnected()
    }

    abstract fun onServiceConnected()

    abstract fun onServiceDisconnected()

    fun startRangingBeaconsInRegion(uuid: UUID, listener: BeaconServiceConnection.BeaconsListener): Boolean {
        return startRangingBeaconsInRegion(uuid, BeaconRegion.ANY, BeaconRegion.ANY, listener)
    }

    fun startRangingBeaconsInRegion(companyId: Int, uuid: UUID?, listener: BeaconServiceConnection.BeaconsListener): Boolean {
        return startRangingBeaconsInRegion(companyId, uuid, BeaconRegion.ANY, BeaconRegion.ANY, listener)
    }

    fun startRangingBeaconsInRegion(uuid: UUID, major: Int, listener: BeaconServiceConnection.BeaconsListener): Boolean {
        return startRangingBeaconsInRegion(uuid, major, BeaconRegion.ANY, listener)
    }

    fun startRangingBeaconsInRegion(companyId: Int, uuid: UUID, major: Int, listener: BeaconServiceConnection.BeaconsListener): Boolean {
        return startRangingBeaconsInRegion(companyId, uuid, major, BeaconRegion.ANY, listener)
    }

    fun startRangingBeaconsInRegion(uuid: UUID?, major: Int, minor: Int, listener: BeaconServiceConnection.BeaconsListener?): Boolean {
        return if (mService == null) {
            false
        } else if (listener == null) {
            throw NullPointerException("BeaconsListener may not by null.")
        } else if (uuid == null && (major != BeaconRegion.ANY || minor != BeaconRegion.ANY)) {
            throw UnsupportedOperationException("UUID may not be null if major or minor number is specified.")
        } else if (major == BeaconRegion.ANY && minor != BeaconRegion.ANY) {
            throw UnsupportedOperationException("Minor number may not be specified if major number is not.")
        } else {
            try {
                var messenger: Messenger? = mBeaconsMessengers[listener]
                if (messenger == null) {
                    messenger = Messenger(BeaconServiceConnection.BeaconsListenerHandler(listener, mRegionsByHash, mBeaconsByAddressHash))
                }

                val msg = Message.obtain()
                msg.what = ServiceProxy.MSG_START_RANGING_BEACONS_IN_REGION
                msg.obj = if (uuid != null) ParcelUuid(uuid) else null
                msg.arg1 = major
                msg.arg2 = minor
                msg.replyTo = messenger
                mService!!.send(msg)
                mBeaconsMessengers[listener] = messenger
                true
            } catch (var7: RemoteException) {
                Log.e(TAG, "An exception occurred while sending message", var7)
                false
            }

        }
    }

    fun startRangingBeaconsInRegion(companyId: Int, uuid: UUID?, major: Int, minor: Int, listener: BeaconServiceConnection.BeaconsListener?): Boolean {
        return if (mService == null) {
            false
        } else if (listener == null) {
            throw NullPointerException("BeaconsListener may not by null.")
        } else if (uuid == null && (major != BeaconRegion.ANY || minor != BeaconRegion.ANY)) {
            throw UnsupportedOperationException("UUID may not be null if major or minor number is specified.")
        } else if (major == BeaconRegion.ANY && minor != BeaconRegion.ANY) {
            throw UnsupportedOperationException("Minor number may not be specified if major number is not.")
        } else {
            try {
                var messenger: Messenger? = mBeaconsMessengers[listener]
                if (messenger == null) {
                    messenger = Messenger(BeaconServiceConnection.BeaconsListenerHandler(listener, mRegionsByHash, mBeaconsByAddressHash))
                }

                val msg = Message.obtain()
                msg.what = ServiceProxy.MSG_START_RANGING_BEACONS_IN_REGION
                msg.obj = if (uuid != null) ParcelUuid(uuid) else null
                msg.arg1 = major
                msg.arg2 = minor
                msg.data.putInt(ServiceProxy.EXTRA_COMPANY_ID, companyId)
                msg.replyTo = messenger
                mService!!.send(msg)
                mBeaconsMessengers[listener] = messenger
                true
            } catch (var8: RemoteException) {
                Log.e(TAG, "An exception occurred while sending message", var8)
                false
            }

        }
    }

    fun stopRangingBeaconsInRegion(listener: BeaconServiceConnection.BeaconsListener): Boolean {
        if (mService == null) {
            return false
        } else {
            val messenger = mBeaconsMessengers.remove(listener)
            return if (messenger == null) {
                true
            } else {
                try {
                    val msg = Message.obtain()
                    msg.what = ServiceProxy.MSG_STOP_RANGING_BEACONS_IN_REGION
                    msg.replyTo = messenger
                    mService!!.send(msg)
                    true
                } catch (var4: RemoteException) {
                    Log.e(TAG, "An exception occurred while sending message", var4)
                    false
                }

            }
        }
    }

    fun startMonitoringForRegion(uuid: UUID, listener: BeaconServiceConnection.RegionListener): Boolean {
        return startMonitoringForRegion(uuid, BeaconRegion.ANY, BeaconRegion.ANY, listener)
    }

    fun startMonitoringForRegion(companyId: Int, uuid: UUID, listener: BeaconServiceConnection.RegionListener): Boolean {
        return startMonitoringForRegion(companyId, uuid, BeaconRegion.ANY, BeaconRegion.ANY, listener)
    }

    fun startMonitoringForRegion(uuid: UUID, major: Int, listener: BeaconServiceConnection.RegionListener): Boolean {
        return startMonitoringForRegion(uuid, major, BeaconRegion.ANY, listener)
    }

    fun startMonitoringForRegion(companyId: Int, uuid: UUID, major: Int, listener: BeaconServiceConnection.RegionListener): Boolean {
        return startMonitoringForRegion(companyId, uuid, major, BeaconRegion.ANY, listener)
    }

    fun startMonitoringForRegion(uuid: UUID?, major: Int, minor: Int, listener: BeaconServiceConnection.RegionListener?): Boolean {
        return if (mService == null) {
            false
        } else if (listener == null) {
            throw NullPointerException("RegionListener may not by null.")
        } else if (uuid == null && (major != BeaconRegion.ANY || minor != BeaconRegion.ANY)) {
            throw UnsupportedOperationException("UUID may not be null if major or minor number is specified.")
        } else if (major == BeaconRegion.ANY && minor != BeaconRegion.ANY) {
            throw UnsupportedOperationException("Minor number may not be specified if major number is not.")
        } else {
            try {
                var messenger: Messenger? = mRegionMessengers[listener]
                if (messenger == null) {
                    messenger = Messenger(BeaconServiceConnection.RegionListenerHandler(listener, mRegionsByHash))
                }

                val msg = Message.obtain()
                msg.what = ServiceProxy.MSG_START_MONITORING_FOR_REGION
                msg.obj = if (uuid != null) ParcelUuid(uuid) else null
                msg.arg1 = major
                msg.arg2 = minor
                msg.replyTo = messenger
                mService!!.send(msg)
                mRegionMessengers[listener] = messenger
                true
            } catch (var7: RemoteException) {
                Log.e(TAG, "An exception occurred while sending message", var7)
                false
            }

        }
    }

    fun startMonitoringForRegion(companyId: Int, uuid: UUID?, major: Int, minor: Int, listener: BeaconServiceConnection.RegionListener?): Boolean {
        return if (mService == null) {
            false
        } else if (listener == null) {
            throw NullPointerException("RegionListener may not by null.")
        } else if (uuid == null && (major != BeaconRegion.ANY || minor != BeaconRegion.ANY)) {
            throw UnsupportedOperationException("UUID may not be null if major or minor number is specified.")
        } else if (major == BeaconRegion.ANY && minor != BeaconRegion.ANY) {
            throw UnsupportedOperationException("Minor number may not be specified if major number is not.")
        } else {
            try {
                var messenger: Messenger? = mRegionMessengers[listener]
                if (messenger == null) {
                    messenger = Messenger(BeaconServiceConnection.RegionListenerHandler(listener, mRegionsByHash))
                }

                val msg = Message.obtain()
                msg.what = ServiceProxy.MSG_START_MONITORING_FOR_REGION
                msg.obj = if (uuid != null) ParcelUuid(uuid) else null
                msg.arg1 = major
                msg.arg2 = minor
                msg.data.putInt(ServiceProxy.EXTRA_COMPANY_ID, companyId)
                msg.replyTo = messenger
                mService!!.send(msg)
                mRegionMessengers[listener] = messenger
                true
            } catch (var8: RemoteException) {
                Log.e(TAG, "An exception occurred while sending message", var8)
                false
            }

        }
    }

    fun stopMonitoringForRegion(listener: BeaconServiceConnection.RegionListener): Boolean {
        if (mService == null) {
            return false
        } else {
            val messenger = mRegionMessengers.remove(listener)
            return if (messenger == null) {
                true
            } else {
                try {
                    val msg = Message.obtain()
                    msg.what = ServiceProxy.MSG_STOP_MONITORING_FOR_REGION
                    msg.replyTo = messenger
                    mService!!.send(msg)
                    true
                } catch (var4: RemoteException) {
                    Log.e(TAG, "An exception occurred while sending message", var4)
                    false
                }

            }
        }
    }

    private class RegionListenerHandler(private val mListener: BeaconServiceConnection.RegionListener, private val mRegionsByHash: SparseArray<BeaconRegion>) : Handler() {

        override fun handleMessage(msg: Message) {
            val companyId: Int
            val uuid: UUID?
            val major: Int
            val minor: Int
            val hash: Int
            val region: BeaconRegion
            when (msg.what) {
                ServiceProxy.MSG_REGION_ENTERED -> {
                    uuid = if (msg.obj != null) (msg.obj as ParcelUuid).uuid else null
                    major = msg.arg1
                    minor = msg.arg2
                    companyId = msg.data.getInt(ServiceProxy.EXTRA_COMPANY_ID)
                    hash = if (uuid != null) uuid.hashCode() xor (major shl 16) xor minor else 0
                    region = mRegionsByHash.get(hash, BeaconRegion(companyId, uuid, major, minor))
                    mRegionsByHash.put(hash, region)
                    mListener.onEnterRegion(region)
                }
                ServiceProxy.MSG_REGION_EXITED -> {
                    uuid = if (msg.obj != null) (msg.obj as ParcelUuid).uuid else null
                    major = msg.arg1
                    minor = msg.arg2
                    companyId = msg.data.getInt(ServiceProxy.EXTRA_COMPANY_ID)
                    hash = if (uuid != null) uuid.hashCode() xor (major shl 16) xor minor else 0
                    region = mRegionsByHash.get(hash, BeaconRegion(companyId, uuid, major, minor))
                    mRegionsByHash.put(hash, region)
                    mListener.onExitRegion(region)
                }
            }

        }
    }

    private class BeaconsListenerHandler(private val mListener: BeaconServiceConnection.BeaconsListener, private val mRegionsByHash: SparseArray<BeaconRegion>, private val mBeaconsByAddressHash: SparseArray<Beacon>) : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ServiceProxy.MSG_BEACONS_IN_REGION -> {
                    val regionUuid = if (msg.obj != null) (msg.obj as ParcelUuid).uuid else null
                    val regionMajor = msg.arg1
                    val regionMinor = msg.arg2
                    val companyId = msg.data.getInt(ServiceProxy.EXTRA_COMPANY_ID)
                    val hash = if (regionUuid != null) regionUuid.hashCode() xor (regionMajor shl 16) xor regionMinor else 0
                    val region = mRegionsByHash.get(hash, BeaconRegion(companyId, regionUuid, regionMajor, regionMinor))

                    mRegionsByHash.put(hash, region)
                    val beaconsCache = mBeaconsByAddressHash
                    val beaconsCount = msg.data.getInt(ServiceProxy.EXTRA_COUNT)
                    val beacons: MutableList<Beacon> = ArrayList(beaconsCount)

                    if (beaconsCount > 0) {
                        val addresses = msg.data.getStringArray(ServiceProxy.EXTRA_ADDRESSES) as Array<String>
                        val uuids = msg.data.getParcelableArray(ServiceProxy.EXTRA_UUIDS) as Array<Parcelable>
                        val numbers = msg.data.getIntArray(ServiceProxy.EXTRA_NUMBERS) as IntArray
                        val accuracies = msg.data.getFloatArray(ServiceProxy.EXTRA_ACCURACIES) as FloatArray
                        val rssis = msg.data.getIntArray(ServiceProxy.EXTRA_RSSI_VALUES) as IntArray

                        for (i in 0 until beaconsCount) {
                            val address = addresses[i]
                            val beacon = beaconsCache.get(address.hashCode(), Beacon(address)) as Beacon
                            beaconsCache.put(address.hashCode(), beacon)
                            beacon.uuid = (uuids[i] as ParcelUuid).uuid
                            beacon.major = numbers[i].ushr(16)
                            beacon.minor = numbers[i]
                            beacon.previousAccuracy = beacon.accuracy
                            beacon.accuracy = accuracies[i]
                            beacon.rssi = rssis[i]
                            beacon.companyId = region.major
                            beacons.add(beacon)
                        }
                    }

                    mListener.onBeaconsInRegion(beacons, region)
                }
            }
        }
    }

    interface RegionListener {
        fun onEnterRegion(region: BeaconRegion)

        fun onExitRegion(region: BeaconRegion)
    }

    interface BeaconsListener {
        fun onBeaconsInRegion(beacons: List<Beacon>, region: BeaconRegion)
    }

    companion object {
        private val TAG = "BeaconServiceConnection"
    }
}