package com.otech.ibeacon.v21


import android.os.Handler
import android.os.Message
import android.os.ParcelUuid
import android.util.Log
import com.otech.ibeacon.ServiceProxy
import java.util.*

class BeaconLocationServiceHandler(private val mService: IBeaconLocationService) : Handler() {

    override fun handleMessage(msg: Message) {
        if (!mService.isAlive) {
            Log.w(TAG, "The target service is dead.")
        } else {
            val uuid: UUID?
            val major: Int
            val minor: Int
            val companyId: Int
            when (msg.what) {
                ServiceProxy.MSG_STOP_MONITORING_FOR_REGION -> mService.stopMonitoringForRegion(msg.replyTo)
                ServiceProxy.MSG_START_MONITORING_FOR_REGION -> {
                    uuid = if (msg.obj != null) (msg.obj as ParcelUuid).uuid else null
                    major = msg.arg1
                    minor = msg.arg2
                    companyId = msg.data.getInt(ServiceProxy.EXTRA_COMPANY_ID, ServiceProxy.COMPANY_ID_DEFAULT)
                    mService.startMonitoringForRegion(uuid, major, minor, companyId, msg.replyTo)
                }
                ServiceProxy.MSG_STOP_RANGING_BEACONS_IN_REGION -> mService.stopRangingBeaconsInRegion(msg.replyTo)
                ServiceProxy.MSG_START_RANGING_BEACONS_IN_REGION -> {
                    uuid = if (msg.obj != null) (msg.obj as ParcelUuid).uuid else null
                    major = msg.arg1
                    minor = msg.arg2
                    companyId = msg.data.getInt(ServiceProxy.EXTRA_COMPANY_ID, ServiceProxy.COMPANY_ID_DEFAULT)
                    mService.startRangingBeaconsInRegion(uuid, major, minor, companyId, msg.replyTo)
                }
            }

        }
    }

    companion object {
        private val TAG = "BeaconLocationServiceH"
    }
}
