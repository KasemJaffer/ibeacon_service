package co.xtrava.ibeacon.v21


import android.annotation.TargetApi
import android.os.Messenger
import co.xtrava.ibeacon.v21.utils.DebugLogger
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*

@TargetApi(21)
class RangingRegionsCollection {
    private val mRegions = ArrayList<BeaconRegionState>()
    private val mDeadRegions = ArrayList<BeaconRegionState>()

    val regions: List<BeaconRegionState>
        get() = mRegions

    val isEmpty: Boolean
        get() = synchronized(mRegions) {
            return mRegions.size == 0
        }

    fun addRegion(uuid: UUID?, major: Int, minor: Int, companyId: Int, messenger: Messenger) {
        val region = BeaconRegionState(uuid, major, minor, companyId, messenger)
        DebugLogger.i(TAG, "Adding region for ranging: $region")
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
        mRegions.removeAll {
            if (it.matches(messenger)) {
                DebugLogger.i(TAG, "Removing region for ranging: $it")
                return@removeAll true
            }
            return@removeAll false
        }
    }

    fun markBeaconFound(result: ScanResult) {
        synchronized(mRegions) {
            for (region in mRegions) {
                if (region.matches(result)) {
                    region.beaconFound(result)
                    DebugLogger.d(TAG, "Beacon found in region " + region + ", rssi: " + result.rssi)
                }
            }
        }
    }

    fun sendBeaconsInRegionsNotification() {
        for (region in mRegions) {
            if (!region.sendBeaconsInRegionNotification()) {
                mDeadRegions.add(region)
            }
        }
    }

    fun removeDeadRegions() {
        for (region in mDeadRegions) {
            mRegions.remove(region)
        }
        mDeadRegions.clear()
    }

    fun clear() {
        synchronized(mRegions) {
            mRegions.clear()
        }
    }

    companion object {
        private val TAG = "RangingRegionsCollection"
    }
}
