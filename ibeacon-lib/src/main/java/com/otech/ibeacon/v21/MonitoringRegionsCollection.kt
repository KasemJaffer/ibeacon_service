package com.otech.ibeacon.v21


import android.app.Notification
import android.app.NotificationManager
import android.os.Messenger
import android.os.SystemClock
import com.otech.ibeacon.v21.utils.DebugLogger
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*

class MonitoringRegionsCollection {
    private val mRegions = ArrayList<BeaconRegionState>()
    private val mRegionsEntered = ArrayList<BeaconRegionState>()
    private val mRegionsExited = ArrayList<BeaconRegionState>()
    private val mDeadRegions = ArrayList<BeaconRegionState>()


    val regions: List<BeaconRegionState>
        get() = mRegions

    val isEmpty: Boolean
        get() = synchronized(mRegions) {
            return mRegions.isEmpty()
        }

    fun addRegion(uuid: UUID, major: Int, minor: Int, companyId: Int, ownerPackageName: String, notification: Notification) {
        val region = BeaconRegionState(uuid, major, minor, companyId, ownerPackageName, notification)
        DebugLogger.i(TAG, "Adding region for background monitoring: $region")
        synchronized(mRegions) {
            val regions = mRegions
            val index = regions.indexOf(region)
            if (index != -1) {
                regions[index] = region
            } else {
                regions.add(region)
            }
            return@synchronized
        }
    }

    fun removeRegion(uuid: UUID, major: Int, minor: Int, companyId: Int, ownerPackageName: String) {
        synchronized(mRegions) {
            mRegions.removeAll {
                if (it.matchesStrict(uuid.mostSignificantBits, uuid.leastSignificantBits, major.toLong(), minor.toLong(), companyId, ownerPackageName)) {
                    return@removeAll true
                }
                return@removeAll false
            }
        }
    }

    fun addRegion(uuid: UUID?, major: Int, minor: Int, companyId: Int, messenger: Messenger) {
        val region = BeaconRegionState(uuid, major, minor, companyId, messenger)
        DebugLogger.i(TAG, "Adding region for monitoring: $region")

        synchronized(mRegions) {
            val index = mRegions.indexOf(region)
            if (index != -1) {
                mRegions[index] = region
            } else {
                mRegions.add(region)
            }
            return@synchronized
        }
    }

    fun removeRegion(messenger: Messenger) {
        synchronized(mRegions) {
            mRegions.removeAll {
                if (it.matches(messenger)) {
                    DebugLogger.i(TAG, "Removing region for active monitoring: $it")
                    return@removeAll true
                }
                return@removeAll false
            }
        }
    }

    fun removeActiveRegions() {
        synchronized(mRegions) {
            mRegions.removeAll {
                if (it.hasMessenger()) {
                    DebugLogger.i(TAG, "Removing active monitoring region: $it")
                    return@removeAll true
                }
                return@removeAll false
            }
        }
    }

    fun markRegionsFound(result: ScanResult) {
        synchronized(mRegions) {
            mRegions.forEach {
                if (it.matches(result) && it.hasJustEntered()) {
                    mRegionsEntered.add(it)
                    DebugLogger.d(TAG, "Beacon in range $it")
                }
            }
        }
    }

    fun sendRegionEnteredNotifications(notificationManager: NotificationManager) {
        if (!mRegionsEntered.isEmpty()) {
            DebugLogger.d(TAG, "Regions entered: $mRegionsEntered")
        }

        for (region in mRegionsEntered) {
            if (!region.sendRegionEnteredNotification(notificationManager)) {
                mDeadRegions.add(region)
            }
        }

    }

    fun sendRegionExitedNotifications(notificationManager: NotificationManager) {
        val currentTimestamp = SystemClock.elapsedRealtime()

        mRegions.forEach {
            if (it.hasJustExited(currentTimestamp)) {
                mRegionsExited.add(it)
            }
        }


        if (!mRegionsExited.isEmpty()) {
            DebugLogger.d(TAG, "Regions exited: $mRegionsExited")
        }

        mRegionsExited.forEach {
            if (!it.sendRegionExitedNotification(notificationManager)) {
                mDeadRegions.add(it)
            }
        }
    }

    fun removeDeadRegions() {

        for (region in mDeadRegions) {
            mRegions.remove(region)
        }

        mDeadRegions.clear()
    }

    fun clearNotifiedRegions() {
        mRegionsEntered.clear()
        mRegionsExited.clear()
    }

    fun clear() {
        synchronized(mRegions) {
            mRegions.clear()
        }
    }

    companion object {
        private val TAG = "MonitoringRegionsCollection"
    }
}