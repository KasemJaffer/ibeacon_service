package com.otech.ibeacon.v21

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import com.otech.ibeacon.BeaconRegion
import com.otech.ibeacon.ServiceProxy
import com.otech.ibeacon.v21.utils.DebugLogger
import no.nordicsemi.android.support.v18.scanner.*
import java.util.*


class BeaconLocationService : Service(), SharedPreferences.OnSharedPreferenceChangeListener, IBeaconLocationService {
    private val TAG = "BeaconLocationService"
    private val SCAN_ENABLED_KEY = "scan_enabled"
    private val ONGOING_NOTIFICATION_ID = 1
    private val SECOND = 1000L
    private val NOTIFY_INTERVAL = 5000L
    private val NOTIFY_INTERVAL_BINDED = 2000L
    private val mMonitoringRegions = MonitoringRegionsCollection()
    private val mRangingRegions = RangingRegionsCollection()
    private var mStarted: Boolean = false
    private var mBinded: Boolean = false
    private lateinit var mNotificationManager: NotificationManager
    private var mAdapter: BluetoothAdapter? = null
    private var mScanner: BluetoothLeScannerCompat? = null
    private var mBinderMessenger: Messenger? = null
    private var mHandler: Handler? = null

    private val notifyAppTask = object : Runnable {
        override fun run() {
            if (mStarted) {
                mMonitoringRegions.sendRegionEnteredNotifications(mNotificationManager)
                mMonitoringRegions.sendRegionExitedNotifications(mNotificationManager)
                mMonitoringRegions.clearNotifiedRegions()
                mMonitoringRegions.removeDeadRegions()
                if (mBinded) {
                    val rangingRegions = mRangingRegions
                    rangingRegions.sendBeaconsInRegionsNotification()
                    rangingRegions.removeDeadRegions()
                }
                mHandler?.postDelayed(this, if (mBinded) NOTIFY_INTERVAL_BINDED else NOTIFY_INTERVAL)
            }
        }

    }

    private val scanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            Log.i(TAG, "onBatchScanResults: $results")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            mMonitoringRegions.markRegionsFound(result)
            mRangingRegions.markBeaconFound(result)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "onScanFailed: $errorCode")
        }
    }

    private val mBluetoothStateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            when (state) {
                BluetoothAdapter.STATE_OFF -> stopLeScan()
                BluetoothAdapter.STATE_ON -> {
                    val preferences = PreferenceManager.getDefaultSharedPreferences(this@BeaconLocationService)
                    if ((!mMonitoringRegions.isEmpty || !mRangingRegions.isEmpty) && (mBinded || preferences.getBoolean(SCAN_ENABLED_KEY, true))) {
                        startLeScan()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mAdapter = bm.adapter
        mScanner = BluetoothLeScannerCompat.getScanner()
        mHandler = Handler()
        mMonitoringRegions.clear()
        mRangingRegions.clear()
        mStarted = false
        registerReceiver(mBluetoothStateBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        stopLeScan()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        unregisterReceiver(mBluetoothStateBroadcastReceiver)
        mHandler = null
        mAdapter = null
        mScanner = null
        mBinderMessenger = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.d(TAG, "intent = null")
            return Service.START_NOT_STICKY
        } else {
            val action = intent.action
            try {
                val major: Int
                val minor: Int
                val companyId: Int
                if (Intent.ACTION_INSERT == action) {
                    val notification = intent.getParcelableExtra<Notification>(ServiceProxy.EXTRA_NOTIFICATION)
                    if (notification != null && intent.hasExtra(ServiceProxy.EXTRA_UUID) && intent.hasExtra(ServiceProxy.EXTRA_PACKAGE_NAME)) {
                        val uuid = (intent.getParcelableExtra<Parcelable>(ServiceProxy.EXTRA_UUID) as ParcelUuid).uuid
                        major = intent.getIntExtra(ServiceProxy.EXTRA_MAJOR, BeaconRegion.ANY)
                        minor = intent.getIntExtra(ServiceProxy.EXTRA_MINOR, BeaconRegion.ANY)
                        companyId = intent.getIntExtra(ServiceProxy.EXTRA_COMPANY_ID, ServiceProxy.COMPANY_ID_DEFAULT)
                        val packageName = intent.getStringExtra(ServiceProxy.EXTRA_PACKAGE_NAME)
                        mMonitoringRegions.addRegion(uuid, major, minor, companyId, packageName, notification)
                        startLeScanIfEnabled()
                    }
                } else if (Intent.ACTION_DELETE == action) {
                    if (intent.hasExtra(ServiceProxy.EXTRA_UUID) && intent.hasExtra(ServiceProxy.EXTRA_PACKAGE_NAME)) {
                        val uuid = (intent.getParcelableExtra<Parcelable>(ServiceProxy.EXTRA_UUID) as ParcelUuid).uuid
                        major = intent.getIntExtra(ServiceProxy.EXTRA_MAJOR, BeaconRegion.ANY)
                        minor = intent.getIntExtra(ServiceProxy.EXTRA_MINOR, BeaconRegion.ANY)
                        companyId = intent.getIntExtra(ServiceProxy.EXTRA_COMPANY_ID, ServiceProxy.COMPANY_ID_DEFAULT)
                        val packageName = intent.getStringExtra(ServiceProxy.EXTRA_PACKAGE_NAME)
                        mMonitoringRegions.removeRegion(uuid, major, minor, companyId, packageName)
                    }

                    if (mStarted && mMonitoringRegions.isEmpty && mRangingRegions.isEmpty) {
                        stopLeScan()
                    }
                }
            } catch (var14: Exception) {
                Log.w(TAG, "Operation $action failed", var14)
            } finally {
                if (!mBinded && mMonitoringRegions.isEmpty) {
                    stopSelf()
                }

            }

            return Service.START_NOT_STICKY
        }

    }

    override fun onBind(intent: Intent?): IBinder {
        mBinded = true
        if (!mMonitoringRegions.isEmpty) {
            startForeground()
        }

        mBinderMessenger = Messenger(BeaconLocationServiceHandler(this))
        return mBinderMessenger!!.binder
    }

    override fun onRebind(intent: Intent?) {
        mBinded = true
        if (!mMonitoringRegions.isEmpty) {
            startForeground()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mBinded = false
        mBinderMessenger = null
        mMonitoringRegions.removeActiveRegions()
        mRangingRegions.clear()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!mMonitoringRegions.isEmpty && preferences.getBoolean(SCAN_ENABLED_KEY, true)) {
            startLeScanIfEnabled()
        } else {
            stopLeScan()
            stopSelf()
        }

        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

    }

    override val isAlive: Boolean
        get() = mHandler != null

    override fun startRangingBeaconsInRegion(uuid: UUID?, major: Int, minor: Int, companyId: Int, messenger: Messenger) {
        mRangingRegions.addRegion(uuid, major, minor, companyId, messenger)
        startLeScanIfEnabled()
    }

    override fun stopRangingBeaconsInRegion(messenger: Messenger) {
        mRangingRegions.removeRegion(messenger)
        if (mStarted && mMonitoringRegions.isEmpty && mRangingRegions.isEmpty) {
            stopLeScan()
        }
    }

    override fun startMonitoringForRegion(uuid: UUID?, major: Int, minor: Int, companyId: Int, messenger: Messenger) {
        mMonitoringRegions.addRegion(uuid, major, minor, companyId, messenger)
        startLeScanIfEnabled()
    }

    override fun stopMonitoringForRegion(messenger: Messenger) {
        mMonitoringRegions.removeRegion(messenger)
        if (mStarted && mMonitoringRegions.isEmpty && mRangingRegions.isEmpty) {
            stopLeScan()
        }
    }

    private fun startLeScanIfEnabled() {
        if (mStarted) {
            stopLeScan()
        }
        if (!mStarted && mAdapter?.isEnabled == true) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            if (mBinded || preferences.getBoolean(SCAN_ENABLED_KEY, true)) {
                startLeScan()
            }
        }
    }

    private fun startLeScan() {
        if (mStarted) {
            mStarted = true
            startForeground()
        } else {
            startForeground()
            mStarted = true
            DebugLogger.v(TAG, "Scanning started")
            val filters = ArrayList<ScanFilter>()
            synchronized(mMonitoringRegions.regions) {
                mMonitoringRegions.regions.forEach {
                    filters.add(it.filter)
                }
            }

            synchronized(mRangingRegions.regions) {
                mRangingRegions.regions.forEach {
                    filters.add(it.filter)
                }
            }
            val settings = ScanSettings.Builder()
                    .setScanMode(if (!mBinded) ScanSettings.SCAN_MODE_LOW_POWER else ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setUseHardwareBatchingIfSupported(true)
                    .build()
            mScanner?.startScan(filters, settings, scanCallback)
            mHandler?.removeCallbacks(notifyAppTask)
            mHandler?.postDelayed(notifyAppTask, if (mBinded) NOTIFY_INTERVAL_BINDED else NOTIFY_INTERVAL)
        }
    }

    private fun stopLeScan() {
        DebugLogger.v(TAG, "Stopping scanning...")
        mStarted = false

        try {
            mScanner?.stopScan(scanCallback)
        } catch (var2: Exception) {
            //ignored
        }

        stopForeground(true)
    }


    private fun startForeground() {
        val builder: Notification.Builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Notification.Builder(this, ServiceProxy.CHANNEL_ID)
        } else {
            Notification.Builder(this).setSound(null, null)
        }
        builder.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("Butterfly Service is running")
                .setOngoing(true)
        if (!mBinded) {
            builder.setContentText("Scanning for Butterfly.")
        } else {
            builder.setContentText("Monitoring Butterfly continuously.")
        }

        startForeground(ONGOING_NOTIFICATION_ID, builder.build())
    }

}