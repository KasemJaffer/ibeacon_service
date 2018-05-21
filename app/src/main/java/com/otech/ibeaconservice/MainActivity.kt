package com.otech.ibeaconservice

import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.otech.ibeacon.Beacon
import com.otech.ibeacon.BeaconRegion
import com.otech.ibeacon.BeaconServiceConnection
import com.otech.ibeacon.ServiceProxy

class MainActivity : AppCompatActivity(), BeaconServiceConnection.BeaconsListener {

    private val TAG = localClassName
    private var mServiceConnected: Boolean = false

    private val mServiceConnection = object : BeaconServiceConnection() {
        override fun onServiceConnected() {
            mServiceConnected = true
            startScanning()
        }

        override fun onServiceDisconnected() {
            mServiceConnected = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceProxy.createIBeaconNotificationChannel(this, "iBeacon Service", "Monitors iBeacons")
        }

        startScanning()
    }

    override fun onBeaconsInRegion(beacons: List<Beacon>, region: BeaconRegion) {
        Log.i(TAG, beacons.joinToString { it.toString() })
    }

    fun startScanning() {
        when {
            mServiceConnected -> {
                mServiceConnection.stopRangingBeaconsInRegion(this)
                Log.i(TAG, "Searching for beacons...")
                mServiceConnection.startRangingBeaconsInRegion(ServiceProxy.COMPANY_ID_DEFAULT, BeaconRegion.ANY_UUID, this@MainActivity)
            }
            else -> {
                bindService()
            }
        }
    }

    /**
     * Binds the app with the beacons service.
     */
    private fun bindService() {
        ServiceProxy.bindService(this, mServiceConnection)
    }

    private fun unbindService() {
        if (mServiceConnected) {
            // Unbinding service will stop all active scanning listeners
            ServiceProxy.unbindService(this, mServiceConnection)
        }
    }

    override fun onDestroy() {
        unbindService()
        super.onDestroy()
    }
}
