package co.xtrava.ibeacon

import java.util.*


class BeaconRegion internal constructor(val uuid: UUID?, val major: Int, val minor: Int) {

    override fun toString(): String {
        return uuid.toString() + " major: " + major + " minor: " + minor
    }

    override fun equals(other: Any?): Boolean {
        return try {
             val o = other as BeaconRegion?
            uuid == o!!.uuid && major == o.major && minor == o.minor
        } catch (var3: ClassCastException) {
            false
        }

    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + major
        result = 31 * result + minor
        return result
    }

    companion object {
        val ANY_UUID: UUID? = null
        val ANY = -1
    }
}
