package com.otech.ibeacon

import java.util.*


class BeaconRegion internal constructor(val companyId: Int, val uuid: UUID?, val major: Int, val minor: Int) {

    override fun toString(): String {
        return "companyId: $companyId uuid: $uuid major: $major minor: $minor"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BeaconRegion

        if (companyId != other.companyId) return false
        if (uuid != other.uuid) return false
        if (major != other.major) return false
        if (minor != other.minor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = companyId
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + major
        result = 31 * result + minor
        return result
    }


    companion object {
        val ANY_UUID: UUID? = null
        val ANY = -1
    }
}
