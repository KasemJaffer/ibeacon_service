package com.otech.ibeacon.v21

import android.os.Messenger
import java.util.*

interface IBeaconLocationService {

    val isAlive: Boolean

    fun startRangingBeaconsInRegion(uuid: UUID?, major: Int, minor: Int, companyId: Int, messenger: Messenger)

    fun stopRangingBeaconsInRegion(messenger: Messenger)

    fun startMonitoringForRegion(uuid: UUID?, major: Int, minor: Int, companyId: Int, messenger: Messenger)

    fun stopMonitoringForRegion(messenger: Messenger)
}