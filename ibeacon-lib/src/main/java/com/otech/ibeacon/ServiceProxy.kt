package com.otech.ibeacon

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.support.annotation.RequiresApi
import com.otech.ibeacon.v21.BeaconLocationService
import java.util.*


class ServiceProxy {
    companion object {

        val COMPANY_ID_DEFAULT = 0x004C
        val CHANNEL_ID = "ibeacon"

        val REGION_DIR_MIME_TYPE = "vnd.android.cursor.dir/vnd.com.otech.beacon.region"
        val ACTION_BIND = "com.otech.android.beacon.action.BIND"
        val EXTRA_UUID = "com.otech.android.beacon.extra.UUID"
        val EXTRA_MAJOR = "com.otech.android.beacon.extra.MAJOR"
        val EXTRA_MINOR = "com.otech.android.beacon.extra.MINOR"
        val EXTRA_COMPANY_ID = "com.otech.android.beacon.extra.COMPANY_ID"
        val EXTRA_PACKAGE_NAME = "com.otech.android.beacon.extra.PACKAGE_NAME"
        val EXTRA_NOTIFICATION = "com.otech.android.beacon.extra.NOTIFICATION"
        val EXTRA_COUNT = "com.otech.android.beacon.extra.COUNT"
        val EXTRA_ADDRESSES = "com.otech.android.beacon.extra.ADDRESSES"
        val EXTRA_UUIDS = "com.otech.android.beacon.extra.UUIDS"
        val EXTRA_NUMBERS = "com.otech.android.beacon.extra.NUMBERS"
        val EXTRA_ACCURACIES = "com.otech.android.beacon.extra.ACCURACIES"
        val EXTRA_RSSI_VALUES = "com.otech.android.beacon.extra.RSSI_VALUES"

        val MSG_STOP_MONITORING_FOR_REGION = 10
        val MSG_START_MONITORING_FOR_REGION = 11
        val MSG_REGION_ENTERED = 12
        val MSG_REGION_EXITED = 13
        val MSG_STOP_RANGING_BEACONS_IN_REGION = 20
        val MSG_START_RANGING_BEACONS_IN_REGION = 21
        val MSG_BEACONS_IN_REGION = 22


        fun startMonitoringForRegion(context: Context, uuid: UUID, notification: Notification): Boolean {
            return startMonitoringForRegion(context, uuid, BeaconRegion.ANY, BeaconRegion.ANY, notification)
        }

        fun startMonitoringForRegion(context: Context, companyId: Int, uuid: UUID, notification: Notification): Boolean {
            return startMonitoringForRegion(context, companyId, uuid, BeaconRegion.ANY, BeaconRegion.ANY, notification)
        }

        fun startMonitoringForRegion(context: Context, uuid: UUID, major: Int, notification: Notification): Boolean {
            return startMonitoringForRegion(context, uuid, major, BeaconRegion.ANY, notification)
        }

        fun startMonitoringForRegion(context: Context, companyId: Int, uuid: UUID, major: Int, notification: Notification): Boolean {
            return startMonitoringForRegion(context, companyId, uuid, major, BeaconRegion.ANY, notification)
        }

        fun startMonitoringForRegion(context: Context?, uuid: UUID?, major: Int, minor: Int, notification: Notification): Boolean {
            if (context == null) {
                throw NullPointerException("Context may not be null")
            } else if (uuid == null) {
                throw NullPointerException("UUID may not be null")
            } else {
                val service = newServiceIntent(context, "android.intent.action.INSERT")
                service.type = ServiceProxy.REGION_DIR_MIME_TYPE
                service.putExtra(ServiceProxy.EXTRA_UUID, ParcelUuid(uuid))
                service.putExtra(ServiceProxy.EXTRA_MAJOR, major)
                service.putExtra(ServiceProxy.EXTRA_MINOR, minor)
                service.putExtra(ServiceProxy.EXTRA_PACKAGE_NAME, context.packageName)
                service.putExtra(ServiceProxy.EXTRA_NOTIFICATION, notification)
                return context.startService(service) != null
            }
        }

        fun startMonitoringForRegion(context: Context?, companyId: Int, uuid: UUID?, major: Int, minor: Int, notification: Notification): Boolean {
            if (context == null) {
                throw NullPointerException("Context may not be null")
            } else if (uuid == null) {
                throw NullPointerException("UUID may not be null")
            } else {
                val service = newServiceIntent(context, "android.intent.action.INSERT")
                service.type = ServiceProxy.REGION_DIR_MIME_TYPE
                service.putExtra(ServiceProxy.EXTRA_UUID, ParcelUuid(uuid))
                service.putExtra(ServiceProxy.EXTRA_MAJOR, major)
                service.putExtra(ServiceProxy.EXTRA_MINOR, minor)
                service.putExtra(ServiceProxy.EXTRA_COMPANY_ID, companyId)
                service.putExtra(ServiceProxy.EXTRA_PACKAGE_NAME, context.packageName)
                service.putExtra(ServiceProxy.EXTRA_NOTIFICATION, notification)
                return context.startService(service) != null
            }
        }

        @JvmOverloads
        fun stopMonitoringForRegion(context: Context?, uuid: UUID?, major: Int = BeaconRegion.ANY, minor: Int = BeaconRegion.ANY): Boolean {
            if (context == null) {
                throw NullPointerException("Context may not be null")
            } else if (uuid == null) {
                throw NullPointerException("UUID may not be null")
            } else {
                val service = newServiceIntent(context, "android.intent.action.DELETE")
                service.type = ServiceProxy.REGION_DIR_MIME_TYPE
                service.putExtra(ServiceProxy.EXTRA_UUID, ParcelUuid(uuid))
                service.putExtra(ServiceProxy.EXTRA_MAJOR, major)
                service.putExtra(ServiceProxy.EXTRA_MINOR, minor)
                service.putExtra(ServiceProxy.EXTRA_PACKAGE_NAME, context.packageName)
                return context.startService(service) != null
            }
        }

        @JvmOverloads
        fun stopMonitoringForRegion(context: Context?, companyId: Int, uuid: UUID?, major: Int = BeaconRegion.ANY, minor: Int = BeaconRegion.ANY): Boolean {
            if (context == null) {
                throw NullPointerException("Context may not be null")
            } else if (uuid == null) {
                throw NullPointerException("UUID may not be null")
            } else {
                val service = newServiceIntent(context, "android.intent.action.DELETE")
                service.type = ServiceProxy.REGION_DIR_MIME_TYPE
                service.putExtra(ServiceProxy.EXTRA_UUID, ParcelUuid(uuid))
                service.putExtra(ServiceProxy.EXTRA_MAJOR, major)
                service.putExtra(ServiceProxy.EXTRA_MINOR, minor)
                service.putExtra(ServiceProxy.EXTRA_COMPANY_ID, companyId)
                service.putExtra(ServiceProxy.EXTRA_PACKAGE_NAME, context.packageName)
                return context.startService(service) != null
            }
        }

        fun bindService(context: Context?, connection: BeaconServiceConnection): Boolean {
            if (context == null) {
                throw NullPointerException("Context may not be null")
            } else {
                val service = newServiceIntent(context, ServiceProxy.ACTION_BIND)
                return context.bindService(service, connection, Context.BIND_AUTO_CREATE)
            }
        }

        fun unbindService(context: Context?, connection: BeaconServiceConnection) {
            if (context == null) {
                throw NullPointerException("Context may not be null")
            } else {
                context.unbindService(connection)
            }
        }

        private fun newServiceIntent(context: Context, action: String): Intent {
            val service = Intent(action)
            service.setClass(context, BeaconLocationService::class.java)
            return service
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        fun createIBeaconNotificationChannel(context: Context, name: String, description: String) {
            val channel = NotificationChannel(ServiceProxy.CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = description
            channel.setShowBadge(false)
            channel.setSound(null, null)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
