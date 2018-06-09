package com.otech.ibeacon

import java.util.*

internal open class Beacon internal constructor(val deviceAddress: String) {
    var companyId: Int? = null
        internal set
    var uuid: UUID? = null
        internal set
    var major: Int = 0
        internal set
    var minor: Int = 0
        internal set
    var rssi: Int = 0
        internal set
    var accuracy: Float = 0.toFloat()
        internal set
    internal var previousAccuracy: Float = 0.toFloat()

    val proximity: Proximity
        get() = calculateProximity(accuracy)

    val previousProximity: Proximity
        get() = calculateProximity(previousAccuracy)

    init {
        accuracy = -1.0f
        previousAccuracy = -1.0f
    }

    override fun toString(): String {
        return "$uuid major: $major minor: $minor | proximity: $proximity accuracy: ${accuracy}m"
    }

    private fun calculateProximity(accuracy: Float): Proximity {
        return if (accuracy == -1.0f) {
            Proximity.UNKNOWN
        } else if (accuracy.toDouble() <= 0.26) {
            Proximity.IMMEDIATE
        } else {
            if (accuracy.toDouble() <= 2.0) Proximity.NEAR else Proximity.FAR
        }
    }
}